/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Holder of dependency map between BPMN files and external script files.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 30, 2019
 */
@Component
public class DependencyHolder implements AutoCloseable {
    private final Multimap<Path, Path> map = ArrayListMultimap.<Path, Path>create();

    /**
     * Remove dependency link between any scripts and single BPMN file.
     *
     * @param bpmnFile BPMN file to unlink dependencies from
     */
    public void removeDependency(Path bpmnFile) {
        synchronized (map) {
            map.entries().removeIf(e -> e.getValue().equals(bpmnFile));
        }
    }

    /**
     * Add dependency link for every script file in given set to BPMN file.
     *
     * @param scripts list {@link Set} of script files to link
     * @param bpmnFile BPMN file to link scripts to
     */
    public void addDependency(Set<Path> scripts, Path bpmnFile) {
        synchronized (map) {
            scripts.forEach(script -> map.put(script, bpmnFile));
        }
    }

    /**
     * Get collection of all deployed BPMNs that refer to given script.
     *
     * @param script to to check reference for
     * @return {@link Collection} of affected BPMN files.
     */
    public Collection<Path> getAffectedDeployments(Path script) {
        synchronized (map) {
            return map.get(script);
        }
    }

    @Override
    public void close() {
        map.clear();
    }
}
