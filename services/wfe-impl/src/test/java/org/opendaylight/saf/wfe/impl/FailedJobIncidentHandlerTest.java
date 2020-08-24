/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import static org.junit.Assert.assertSame;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test for {@link FailedJobIncidentHandler}.
 *
 * @author <a href="mailto:spichandi@luminanetworks.com">Sowmiya Pichandi</a>
 * @since Dec 18, 2019
 */
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FailedJobIncidentHandlerTest {
    public static final Logger LOG = LoggerFactory.getLogger(FailedJobIncidentHandlerTest.class);
    private static final String PROCESS_DEFINITION_KEY = "FailedJobProcess";

    @ClassRule
    @Rule
    public static ProcessEngineRule processEngineRule = TestCoverageProcessEngineRuleBuilder.create().build();

    private RuntimeService runtimeService;
    private HistoryService historyService;
    private ManagementService managementService;

    @Before
    public void setUp() {
        runtimeService = processEngineRule.getRuntimeService();
        historyService = processEngineRule.getHistoryService();
        managementService = processEngineRule.getManagementService();
    }

    @Test
    @Deployment(resources = "FailedJobProcess.bpmn")
    public void testHappyPath() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
        Job job = managementService.createJobQuery().singleResult();
        try {
            managementService.executeJob(job.getId());
        } catch (ProcessEngineException e) {
            LOG.info("Test failed: Unable to execute the job");
        }
        // Wait until the async job gets executed by job executor
        while (historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .internallyTerminated()
                .singleResult() == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                LOG.info(e1.getMessage());
            }
        }
        assertSame("INTERNALLY_TERMINATED",
                historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult()
                        .getState());
    }

}