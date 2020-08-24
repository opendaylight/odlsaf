/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import static org.opendaylight.saf.codegen.Util.CFG_JAVA_PACKAGE_PREFIX;
import static org.opendaylight.saf.codegen.Util.PROP_JAVA_PACKAGE;
import static org.opendaylight.saf.codegen.Util.getJavaPackage;
import static org.opendaylight.saf.codegen.Util.possibleNodesFilter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang2sources.spi.BasicCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generator entry point.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 11, 2019
 */
@SuppressFBWarnings({ "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" })
public class JavaCodeGeneratorImpl implements BasicCodeGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(JavaCodeGeneratorImpl.class);
    private Map<String, String> config = new HashMap<>();
    private File resourceBaseDir;

    @Override
    public Collection<File> generateSources(EffectiveModelContext context, File outputBaseDir,
            Set<Module> currentModules, Function<Module, Optional<String>> moduleResourcePathResolver)
            throws IOException {
        Preconditions.checkNotNull(config.get(CFG_JAVA_PACKAGE_PREFIX), "Missing option %s", CFG_JAVA_PACKAGE_PREFIX);

        final VelocityContext vc = new VelocityContext();
        vc.put("currentyear", Year.now(ZoneId.of("UTC")).getValue());
        vc.put("currentdatetime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        vc.put("op", "(");
        vc.put("cp", ")");
        vc.put("dot", ".");

        final ImmutableList.Builder<File> result = ImmutableList.builder();
        if (!resourceBaseDir.exists()) {
            Files.createDirectories(resourceBaseDir.toPath());
        }
        for (Module module : currentModules) {
            vc.put(PROP_JAVA_PACKAGE, getJavaPackage(config.get(CFG_JAVA_PACKAGE_PREFIX), module));
            result.addAll(processModule(module, vc));
        }
        return result.build();
    }

    private Iterable<? extends File> processModule(Module module, VelocityContext vc) throws IOException {
        final List<File> result = new ArrayList<>();
        if (!module.getRpcs().isEmpty()) {
            LOG.debug("Found RPCs {}", module.getRpcs());
            result.addAll(new RpcServiceGenerator(resourceBaseDir, module, vc).generate());
            for (RpcDefinition rpc : module.getRpcs()) {
                result.addAll(new RpcIoGenerator(resourceBaseDir, module, vc, rpc).generate());
            }
        }
        if (!module.getNotifications().isEmpty()) {
            LOG.debug("Found Notifications {}", module.getNotifications());
            result.addAll(new NotificationServiceGenerator(resourceBaseDir, module, vc).generate());
            for (NotificationDefinition not : module.getNotifications()) {
                result.addAll(new NotificationPayloadGenerator(resourceBaseDir, module, vc, not).generate());
            }
        }
        for (DataSchemaNode child : module.getChildNodes()
                .stream()
                .filter(possibleNodesFilter())
                .collect(Collectors.toSet())) {
            result.addAll(new DtoGenerator(resourceBaseDir, module, vc, child).generate());
        }
        return result;
    }

    @Override
    public void setAdditionalConfig(Map<String, String> additionalConfiguration) {
        config.putAll(additionalConfiguration);
    }

    @Override
    public void setResourceBaseDir(File resourceBaseDir) {
        this.resourceBaseDir = Objects.requireNonNull(resourceBaseDir);
    }
}
