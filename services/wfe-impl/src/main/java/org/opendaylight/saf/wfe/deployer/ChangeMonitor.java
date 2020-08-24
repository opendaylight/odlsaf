/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.opendaylight.saf.wfe.impl.model.FilesystemChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Check for filesystem changes in workspace directory.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 30, 2019
 */
@Component
public class ChangeMonitor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeMonitor.class);
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true)
            .setNameFormat("bpmn-change-monitor-%d")
            .build();

    @Value("${workspace}")
    private Path workspace;
    @Autowired
    private Consumer<FilesystemChange> eventConsumer;
    private FileAlterationObserver observer;
    private FileAlterationMonitor monitor;

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Watching for changes under {}", workspace);
        observer = new FileAlterationObserver(workspace.toFile());
        monitor = new FileAlterationMonitor(1000L);
        monitor.setThreadFactory(threadFactory);
        // emit initial set of "changes" before our poller loop is started
        try (Stream<Path> paths = Files.walk(workspace, 1).filter(DeployerUtil::isBpmnFile)) {
            paths.forEach(path -> queueChange(new FilesystemChange(path, ENTRY_CREATE)));
        }
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                queueChange(new FilesystemChange(file.toPath(), StandardWatchEventKinds.ENTRY_CREATE));
            }

            @Override
            public void onFileChange(File file) {
                queueChange(new FilesystemChange(file.toPath(), StandardWatchEventKinds.ENTRY_MODIFY));
            }

            @Override
            public void onFileDelete(File file) {
                queueChange(new FilesystemChange(file.toPath(), StandardWatchEventKinds.ENTRY_DELETE));
            }
        });
        monitor.addObserver(observer);
        monitor.start();
    }

    @Override
    public void close() throws Exception {
        monitor.stop();
    }

    private void queueChange(FilesystemChange change) {
        LOG.info("Adding item to queue {}", change);
        eventConsumer.accept(change);
    }
}
