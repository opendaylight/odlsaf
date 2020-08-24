/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import static org.opendaylight.saf.wfe.impl.model.DeploymentItem.create;
import static org.opendaylight.saf.wfe.impl.model.DeploymentItem.delete;

import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendaylight.saf.wfe.impl.model.DeploymentItem;
import org.opendaylight.saf.wfe.impl.model.FilesystemChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is mapping {@link FilesystemChange} into collection of {@link DeploymentItem}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 30, 2019
 */
@Component
public class EventMapper implements Function<FilesystemChange, Collection<DeploymentItem>>, Consumer<FilesystemChange> {
    private static final Logger LOG = LoggerFactory.getLogger(EventMapper.class);
    @Autowired
    private Consumer<DeploymentItem> consumer;
    @Autowired
    private DependencyHolder dependencyHolder;

    @Override
    public Collection<DeploymentItem> apply(FilesystemChange change) {
        final List<DeploymentItem> result = new ArrayList<>();
        if (change.getKind() == StandardWatchEventKinds.ENTRY_DELETE) {
            // can't use DeployerUtil.isBpmnFile here because file no longer exists
            if (change.getPath().toString().endsWith(".bpmn")) {
                result.add(delete(change.getPath()));
            }
            // if given file is script, find all BPMN files that refer to it and undeploy them
            dependencyHolder.getAffectedDeployments(change.getPath()).forEach(bpmn -> result.add(delete(bpmn)));
        }
        // when script is created, then no action is needed, it is taken care of by DeploymentItem
        if (change.getKind() == StandardWatchEventKinds.ENTRY_CREATE && DeployerUtil.isBpmnFile(change.getPath())) {
            result.add(create(change.getPath()));
        }
        if (change.getKind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            if (DeployerUtil.isBpmnFile(change.getPath())) {
                result.add(delete(change.getPath()));
                result.add(create(change.getPath()));
            }
            // if given file is script, find all BPMN files that refer to it and redeploy them
            dependencyHolder.getAffectedDeployments(change.getPath()).forEach(bpmn -> {
                result.add(delete(bpmn));
                result.add(create(bpmn));
            });
        }
        LOG.info("{} was mapped into {}", change, result);
        return result;
    }

    @Override
    public void accept(FilesystemChange change) {
        apply(change).forEach(consumer);
    }
}
