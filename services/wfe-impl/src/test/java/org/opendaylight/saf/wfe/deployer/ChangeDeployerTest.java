/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.exception.DeploymentResourceNotFoundException;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SafWfeRpcService;
import org.opendaylight.saf.wfe.impl.AbstractWorkflowTest;
import org.opendaylight.saf.wfe.impl.WFEApplication;
import org.opendaylight.saf.wfe.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { WFEApplication.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ChangeDeployerTest extends AbstractWorkflowTest {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeDeployerTest.class);
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private SafWfeRpcService handler;

    @Value("${workspace}")
    private Path workspace;

    @Test
    public void test() throws InterruptedException, IOException {
        TestUtils.copyResource("wf1/wf1.bpmn", workspace);
        TestUtils.copyResource("wf1/wf1-script1.py", workspace);
        TestUtils.copyResource("wf1/wf1-script2.py", workspace);

        // await for deployment to be ready
        assertTrue(awaitForProcessDefinition(30, "wf1", true));
        // race condition in runtime service? I've seen once deployment to be ready, but fail to start
        TimeUnit.MILLISECONDS.sleep(250L);

        dumpInfo();

        final ProcessInstance process = runtimeService.startProcessInstanceByKey("wf1");
        waitForJob(handler, process.getId());

        // Update same file
        TestUtils.copyResource("wf1/wf1.bpmn", workspace);

        assertTrue(awaitForProcessDefinition(30, "wf1", true));

        dumpInfo();

        // remove BPMN
        Files.delete(workspace.resolve("wf1.bpmn"));

        // await for deployment to be gone
        assertTrue(awaitForProcessDefinition(30, "wf1", false));

        dumpInfo();
    }

    private void dumpInfo() {
        try {
            repositoryService.createDeploymentQuery().list().forEach(dep -> LOG.info("Deployment : {}", dep));
            repositoryService.createProcessDefinitionQuery()
                    .list()
                    .forEach(proc -> LOG.info("Process definition : {}", proc));
        } catch (DeploymentResourceNotFoundException e) {
            // NOOP, resource was concurrently removed from repository while we iterating over list
        }
    }
}
