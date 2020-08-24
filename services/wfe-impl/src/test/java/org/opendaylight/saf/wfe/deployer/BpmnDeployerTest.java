/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.saf.wfe.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for {@link ChangeDeployer}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jun 15, 2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DeployerTestConfig.class, initializers = DeployerTestConfig.class)
public class BpmnDeployerTest {
    private static final Logger LOG = LoggerFactory.getLogger(BpmnDeployerTest.class);
    @Autowired
    private RepositoryService repositoryService;
    @Value("${workspace}")
    private Path path;

    @Before
    public void setUp() throws IOException, InterruptedException {
        TimeUnit.MILLISECONDS.sleep(200);
    }

    @After
    public void tearDown() {
        reset(repositoryService);
        TestUtils.removeDirectoryRecursive(path);
    }

    @Test
    public void test() throws IOException, InterruptedException {
        Deployment deployment = mock(Deployment.class);
        when(deployment.getId()).thenReturn(UUID.randomUUID().toString());
        DeploymentBuilder db = mock(DeploymentBuilder.class);
        when(db.enableDuplicateFiltering(eq(false))).thenReturn(db);
        when(db.addInputStream(anyString(), any(InputStream.class))).thenReturn(db);
        when(db.name(anyString())).thenReturn(db);
        when(db.deploy()).thenReturn(deployment);
        when(repositoryService.createDeployment()).thenReturn(db);
        Stream.of("test1.bpmn", "1234.py", "abcd.groovy", "456.groovy").forEach(file -> {
            LOG.info("Copy {} to {}", file, path);
            TestUtils.copyResource(file, path);
        });
        TimeUnit.MILLISECONDS.sleep(2000);
        verify(repositoryService, atLeast(1)).createDeployment();
        TestUtils.copyResource("1234.py", path);
        TimeUnit.MILLISECONDS.sleep(250);
        Files.delete(path.resolve("test1.bpmn"));
        TimeUnit.MILLISECONDS.sleep(2000);
        verify(repositoryService, atLeast(1)).createProcessDefinitionQuery();
    }
}
