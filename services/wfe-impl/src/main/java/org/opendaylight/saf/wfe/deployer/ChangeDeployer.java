/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.opendaylight.saf.wfe.impl.model.DeploymentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consume queue of changes and (un)deploy them in {@link RepositoryService}.
 *
 * @author spichandi
 *
 */
@Component
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public class ChangeDeployer implements Consumer<DeploymentItem>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeDeployer.class);
    private final RepositoryService repositoryService;
    private final DependencyHolder dependencyHolder;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1,
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("bpmn-change-deployer-%d").build());

    private Future<?> deployerFuture;
    private AtomicBoolean isPolling = new AtomicBoolean(true);
    private final Map<Path, String> deployments = new ConcurrentHashMap<>();
    private final List<DeploymentItem> queue = Collections.synchronizedList(new ArrayList<>());

    public ChangeDeployer(RepositoryService repositoryService, DependencyHolder dependencyHolder) {
        this.repositoryService = Objects.requireNonNull(repositoryService);
        this.dependencyHolder = Objects.requireNonNull(dependencyHolder);
    }

    @PostConstruct
    public void init() {
        deployerFuture = threadPool.submit(this::processQueue);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void processQueue() {
        try {
            while (isPolling.get()) {
                final List<DeploymentItem> toRemove;
                synchronized (queue) {
                    // remove failed deployments first
                    final List<DeploymentItem> failed = queue.stream()
                            .filter(DeploymentItem::isFailed)
                            .collect(Collectors.toList());
                    if (queue.removeAll(failed)) {
                        LOG.warn("Following deployments failed : {}", failed);
                    }
                    // make a copy to prevent locking of collection during processing
                    toRemove = queue.stream().filter(DeploymentItem::isReady).limit(1).collect(Collectors.toList());
                }
                toRemove.forEach(item -> {
                    LOG.info("Processing {}", item);
                    switch (item.getActionType()) {
                        case CREATE:
                            handleCreate(item);
                            break;

                        case DELETE:
                            handleDelete(item.getMainFile());
                            break;

                        default:
                            LOG.warn("Unrecognized action : {}", item);
                            break;
                    }
                });
                // remove processed items
                synchronized (queue) {
                    queue.removeAll(toRemove);
                }
                TimeUnit.MILLISECONDS.sleep(250L);
            }
        } catch (InterruptedException e) {
            LOG.info("Deployer thread was interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Failure while processing queue", e);
        }
    }

    @Override
    public void close() {
        isPolling.set(false);
        queue.clear();
        deployments.clear();
        deployerFuture.cancel(true);
        threadPool.shutdown();
    }

    private void handleDelete(Path filename) {
        final String deploymentId = deployments.remove(filename);
        if (deploymentId != null) {
            handleDeleteInternal(filename, deploymentId);
        } else {
            LOG.info("No deployment ID for file {}, perhaps undeployed recently", filename);
        }
    }

    @SuppressWarnings("squid:S3864")
    private void handleDeleteInternal(Path filename, String deploymentId) {
        repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .list()
                .stream()
                .peek(pd -> LOG.info("Will remove {} from deployment {}", pd, deploymentId))
                .map(ProcessDefinition::getId)
                .forEach(repositoryService::deleteProcessDefinition);
        LOG.info("Removing deployment {}", deploymentId);
        repositoryService.deleteDeployment(deploymentId);
        dependencyHolder.removeDependency(filename);
        LOG.info("Deployment removed {}", deploymentId);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void handleCreate(DeploymentItem item) {
        try (InputStream is = Files.newInputStream(item.getMainFile())) {
            final DeploymentBuilder deploymentbuilder = repositoryService.createDeployment()
                    .enableDuplicateFiltering(false)
                    .name(item.getMainFile().getFileName().toString())
                    .addInputStream(item.getMainFile().getFileName().toString(),
                            Files.newInputStream(item.getMainFile()));

            for (Path script : item.getScripts()) {
                LOG.info("Adding script {} into deployment from file {}", script, item.getMainFile());
                deploymentbuilder.addInputStream(script.getFileName().toString(), Files.newInputStream(script));
            }
            final String deploymentID = deploymentbuilder.deploy().getId();

            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentID);
            LOG.info("Resources in deployment {} : {}", deploymentID, resources);

            deployments.put(item.getMainFile(), deploymentID);
            dependencyHolder.addDependency(item.getScripts(), item.getMainFile());
            LOG.info("File {} was deployed: {}", item.getMainFile(), deploymentID);
        } catch (IOException e) {
            dependencyHolder.removeDependency(item.getMainFile());
            LOG.warn("Deployment of {} failed", item, e);
        }
    }

    @Override
    public void accept(DeploymentItem item) {
        queue.add(item);
    }
}
