/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SafWfeRpcService;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.StatusInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.StatusOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractWorkflowTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractWorkflowTest.class);
    @Autowired
    protected RepositoryService repositoryService;

    protected JsonElement waitForJob(SafWfeRpcService handler, String jobId) {
        // wait for status to become COMPLETED
        for (;;) {
            StatusOutput statusOutput = handler.status(StatusInput.builder().jobId(jobId).build());

            LOG.info("Status output : {}", statusOutput);
            if ("COMPLETED".equals(statusOutput.getWorkflowStatus())) {
                return statusOutput.getWorkflowOutput();
            }
            if ("INTERNALLY_TERMINATED".equals(statusOutput.getWorkflowStatus())) {
                throw new IllegalStateException("Job terminated");
            }
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        }
    }

    protected boolean awaitForProcessDefinition(long maxSeconds, String name, boolean shouldBePresent) {
        final long now = System.currentTimeMillis();
        final long deadline = now + maxSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            final List<ProcessDefinition> result = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionResourceNameLike(name + "%")
                    .list();
            LOG.info("Matched process definitions : {}", result);
            if (shouldBePresent && !result.isEmpty()) {
                return true;
            }
            if (!shouldBePresent && result.isEmpty()) {
                return true;
            }
            Uninterruptibles.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
        }
        return false;
    }
}
