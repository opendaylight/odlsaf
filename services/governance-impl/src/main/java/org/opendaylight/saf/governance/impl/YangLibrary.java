/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import com.google.common.base.Verify;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.jsonrpc.model.ModuleInfo;
import org.opendaylight.jsonrpc.model.StringYangTextSchemaSource;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.modules.Modules;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.rfc7950.ir.IRSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.TextToIRTransformer;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangModelDependencyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * YANG library implementation.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 1, 2020
 */
@Service
@SuppressFBWarnings(value = {"UPM_UNCALLED_PRIVATE_METHOD",
                             "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"}, justification = "False positive")
public class YangLibrary implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(YangLibrary.class);
    private static final Predicate<Path> MATCH_ALL = t -> true;
    private static final Pattern REVISION_RE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern YANG_MODULE_RE = Pattern
            .compile("([a-zA-Z_]?[\\w.-]+)(@\\d{4}-\\d{2}-\\d{2})?\\.yang");
    private static final Predicate<Path> YANG_FILE_FILTER = name -> YANG_MODULE_RE.matcher(name.toString()).find();
    private final Path yangRoot;
    private final Path publishPath;

    private final LoadingCache<Path, ModuleInfo> moduleInfoCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Path, ModuleInfo>() {
                @Override
                public ModuleInfo load(Path path) throws Exception {
                    LOG.debug("Cache miss (module info) {}", path);
                    final String content = readFile(path);
                    final IRSchemaSource schemaSource = readIR(path.getFileName().toString(), content);
                    return new ModuleInfo(schemaSource.getIdentifier().getName(),
                            schemaSource.getIdentifier().getRevision().map(Revision::toString).orElse(null));
                }
            });

    private final LoadingCache<ModuleCacheKey, Optional<Set<ModuleImport>>> moduleImportCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build(new CacheLoader<ModuleCacheKey, Optional<Set<ModuleImport>>>() {
                @Override
                public Optional<Set<ModuleImport>> load(ModuleCacheKey key) throws Exception {
                    LOG.debug("Cache miss (dependnecy tree) {}", key);
                    final Collection<Path> candidateFiles = findModuleFiles(key.getName(), key.getRevision());
                    LOG.debug("Files that matched key {} : {}", key, candidateFiles);
                    return candidateFiles.stream()
                            .map(YangLibrary::readFile)
                            .map(source -> readIR(key.getName(), source))
                            .map(YangModelDependencyInfo::forIR)
                            .map(YangModelDependencyInfo::getDependencies)
                            .map(x -> ((Set<ModuleImport>) x))
                            .findFirst();
                }
            });

    public YangLibrary(@Value("${yang-root}") Path yangRoot) throws IOException {
        this.yangRoot = Objects.requireNonNull(yangRoot);
        publishPath = yangRoot.resolve("published");
        Files.createDirectories(publishPath);
        LOG.info("YANG Library root : {}", yangRoot);

    }

    @NonNull
    public Optional<String> getSource(String module, String revision) {
        return findModuleFiles(module, revision).stream().findFirst().map(YangLibrary::readFile);
    }

    /**
     * Get direct dependencies (module imports) of given module. If module can't be found, then
     * {@link Optional#empty()} is returned. Resulting list of dependencies includes module itself. This is to honor
     * behavior of original implementation.
     *
     * @param module   module name
     * @param revision module revision
     * @return {@link Optional} list of dependencies.
     */
    @NonNull
    public Optional<List<ModuleInfo>> depends(String module, String revision) {
        final List<ModuleInfo> modules = new ArrayList<>();
        moduleImportCache.getUnchecked(new ModuleCacheKey(module, revision))
                .ifPresent(imports -> modules.addAll(imports.stream()
                        .map(mi -> new ModuleInfo(mi.getModuleName(),
                                mi.getRevision().map(Revision::toString).orElse(null)))
                        .collect(Collectors.toList())));
        modules.add(new ModuleInfo(module, revision));
        return modules.size() == 1 ? Optional.empty() : Optional.of(modules);
    }

    /**
     * Resolve all modules that are needed to create schema context using given module. This includes direct imports
     * and all transitive dependencies. Returned collection is not in any particular order. Note that there is no way
     * to distinguish between "module not found" and "module has no imports" in current implementation (or better say
     * how API was designed).
     *
     * @param module   module name
     * @param revision (optional) revision, can be null.
     * @return {@link List} of all dependent modules.
     */
    @NonNull
    public Collection<ModuleInfo> dependsRecursive(String module, String revision) {
        final Set<ModuleInfo> resolved = new HashSet<>();
        final Deque<ModuleInfo> toResolve = new LinkedList<>();
        toResolve.add(new ModuleInfo(module, revision));
        while (!toResolve.isEmpty()) {
            LOG.info("Resolved : {}", resolved);
            LOG.info("Remaining to resolve : {}", toResolve);
            final ModuleInfo current = toResolve.pop();
            resolved.add(current);
            LOG.info("VVV: Module : {} , Rev {}", current.getModule(), current.getRevision());
            final Set<ModuleInfo> currentImports = moduleImportCache
                    .getUnchecked(new ModuleCacheKey(current.getModule(), current.getRevision()))
                    .flatMap(mods -> Optional.of(mods.stream()
                            .map(m -> new ModuleInfo(m.getModuleName(),
                                    m.getRevision().map(Revision::toString).orElse(null)))
                            .collect(Collectors.toSet())))
                    .orElse(Collections.emptySet())
                    .stream()
                    .collect(Collectors.toSet());
            LOG.debug("Imports of '{}' (before filter) : {}", current.getModule(), currentImports);

            final Set<ModuleInfo> filtered = currentImports.stream()
                    .filter(m -> !resolved.contains(m))
                    .filter(m -> !toResolve.contains(m))
                    .collect(Collectors.toSet());
            LOG.debug("Imports of '{}' (after filter) : {}", current.getModule(), filtered);
            toResolve.addAll(filtered);
        }
        return resolved;
    }

    /**
     * Get list of all YANG modules present in library.
     *
     * @return list of all YANG modules
     */
    @NonNull
    public List<ModuleInfo> listAllYangModules() {
        return findModules(MATCH_ALL);
    }

    /**
     * Find all modules that matches given name and (optionally) revision.
     *
     * @param module   name of module
     * @param revision (optional) revision
     * @return list of modules matching given criteria
     */
    @NonNull
    public List<ModuleInfo> findModules(@NonNull String module, @Nullable String revision) {
        Objects.requireNonNull(module, "Module name can't be NULL");
        return findModules(moduleFilter(module).and(revisionFilter(revision)));
    }

    private List<ModuleInfo> findModules(Predicate<Path> filter) {
        return findModuleFiles(filter).stream().map(moduleInfoCache::getUnchecked).collect(Collectors.toList());
    }

    @NonNull
    public PublishResult publishModules(@NonNull List<Modules> modules) {
        final List<String> failed = new ArrayList<>();
        final List<String> existing = new ArrayList<>();
        final List<String> ok = new ArrayList<>();
        boolean error = true;
        for (Modules module : modules) {
            final String name = module.getModule();
            final String content = module.getSource();
            if (name != null && content != null) {
                final Path yangFile = publishPath.resolve(name + ".yang");
                if (yangFile.toFile().exists()) {
                    existing.add(name);
                } else {
                    try {
                        final byte[] data = content.getBytes(StandardCharsets.UTF_8);
                        LOG.debug("Writing {} bytes to '{}'", data.length, yangFile);
                        Files.write(yangFile, data);
                        ok.add(name);
                        error = false;
                    } catch (IOException e) {
                        LOG.debug("Unable to publish module '{}'", module, e);
                        LOG.warn("Publication of module '{}' failed, ignoring", module);
                        failed.add(name);
                    }
                }
            }
        }
        return new PublishResult(reportPublishResult(failed, existing, ok), error);
    }

    @Override
    public void close() throws Exception {
        Stream.of(moduleImportCache, moduleInfoCache).map(LoadingCache::asMap).forEach(Map::clear);
    }

    private static Predicate<Path> revisionFilter(String revision) {
        if (revision == null) {
            return MATCH_ALL;
        }
        Verify.verify(REVISION_RE.matcher(revision).matches(), "Invalid revision : '%s'", revision);
        return file -> file.toString().endsWith("@" + revision + ".yang");
    }

    private static String readFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read YANG file", e);
        }
    }

    private static Predicate<Path> moduleFilter(String name) {
        return path -> {
            final Matcher matcher = YANG_MODULE_RE.matcher(path.getFileName().toString());
            if (matcher.find()) {
                return matcher.group(1).equals(name);
            }
            return false;
        };
    }

    private static IRSchemaSource readIR(String key, String content) {
        try {
            return TextToIRTransformer.transformText(new StringYangTextSchemaSource(key, content));
        } catch (YangSyntaxErrorException | IOException e) {
            throw new IllegalStateException("Unable to parse YANG IR", e);
        }
    }

    private Collection<Path> findModuleFiles(Predicate<Path> filter) {
        try (Stream<Path> stream = Files.walk(yangRoot).filter(YANG_FILE_FILTER.and(filter))) {
            return stream.collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("Failed to walk directory tree", e);
            return Collections.emptySet();
        }
    }

    private Collection<Path> findModuleFiles(String module, String revision) {
        return findModuleFiles(moduleFilter(module).and(revisionFilter(revision)));
    }

    private static String reportPublishResult(final List<String> failed, final List<String> existing,
            final List<String> ok) {
        final StringBuilder sb = new StringBuilder();
        if (ok.isEmpty()) {
            sb.append("None of modules were published successfuly.");
        } else {
            sb.append("Published : ").append(ok.stream().collect(Collectors.joining(","))).append('.');
        }
        if (!existing.isEmpty()) {
            sb.append("Following modules were skipped as they are already present in library : ")
                    .append(existing.stream().collect(Collectors.joining(",")))
                    .append('.');
        }
        if (!failed.isEmpty()) {
            sb.append("Following modules were not published due to error : ")
                    .append(failed.stream().collect(Collectors.joining(",")))
                    .append('.');
        }
        return sb.toString();
    }
}
