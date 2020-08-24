/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.Revision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods to deal with YANG modules.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Feb 29, 2020
 */
public final class YangModuleUtil {
    private static final String EMPTY = "";
    private static final Logger LOG = LoggerFactory.getLogger(YangModuleUtil.class);

    private YangModuleUtil() {
        // NOOP
    }

    /**
     * Filter set of {@link YangModuleInfo} against regular expression while keeping all dependencies.
     *
     * @param modules set of modules to filter
     * @param modulesFilter regular expression used to filter module names
     * @return filtered set of modules
     */
    static Set<YangModuleInfo> filter(Set<YangModuleInfo> modules, String modulesFilter) {
        final String filterStr = Optional.ofNullable(modulesFilter).orElse(EMPTY).trim();
        if (EMPTY.equals(filterStr)) {
            LOG.info("All {} modules will be part of schema context", modules.size());
            return modules;
        } else {
            LOG.info("Filtering {} modules using pattern '{}'", modules.size(), modulesFilter);
            final Set<String> matched = modules.stream()
                    .map(YangModuleUtil::toSimpleName)
                    .filter(Pattern.compile(filterStr).asPredicate())
                    .collect(Collectors.toSet());
            LOG.debug("Modules that matched filter : {}", matched);

            final Deque<YangModuleInfo> toResolve = new LinkedList<>();
            final Set<YangModuleInfo> resolved = new HashSet<>();
            toResolve.addAll(filterByNames(modules, matched));

            while (!toResolve.isEmpty()) {
                final YangModuleInfo current = toResolve.pop();
                final Collection<YangModuleInfo> imports = current.getImportedModules();
                resolved.add(current);
                toResolve.addAll(imports.stream()
                        .filter(ymi -> !resolved.contains(ymi))
                        .filter(ymi -> !toResolve.contains(ymi))
                        .collect(Collectors.toSet()));
            }
            LOG.info("Resolved {} module(s)", resolved.size());
            return resolved;
        }
    }

    private static Set<YangModuleInfo> filterByNames(Collection<YangModuleInfo> modules, Set<String> names) {
        return modules.stream()
                .filter(ymi -> names.contains(YangModuleUtil.toSimpleName(ymi)))
                .collect(Collectors.toSet());
    }

    static String toSimpleName(YangModuleInfo ymi) {
        return ymi.getName().getLocalName()
                + ymi.getName().getRevision().map(Revision::toString).map(r -> "@" + r).orElse(EMPTY);
    }
}
