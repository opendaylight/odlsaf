/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.impl.cfg.TransactionState;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.incident.DefaultIncidentHandler;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.camunda.bpm.engine.runtime.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Incident handler for Failed Jobs.
 *
 * @author <a href="mailto:spichandi@luminanetworks.com">Sowmiya Pichandi</a>
 * @since Dec 18, 2019
 */
public class FailedJobIncidentHandler extends DefaultIncidentHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FailedJobIncidentHandler.class);

    public FailedJobIncidentHandler(String type) {
        super(type);
        LOG.info("IncidentHandler created for type '{}'", type);
    }

    @Override
    public String getIncidentHandlerType() {
        return Incident.FAILED_JOB_HANDLER_TYPE;
    }

    @Override
    public Incident handleIncident(IncidentContext context, String message) {
        IncidentEntity incidentEntity = (IncidentEntity) super.handleIncident(context, message);
        ExecutionEntity execution = incidentEntity.getExecution();
        LOG.info("Incident {} created for execution {}", incidentEntity.getId(), context.getExecutionId());
        if (execution.getProcessInstanceId() != null) {
            LOG.info("Terminating the process instance with id {}", execution.getProcessInstanceId());
            Context.getCommandContext().getTransactionContext().addTransactionListener(TransactionState.COMMITTED,
                commandContext -> {
                    ProcessEngines.getDefaultProcessEngine().getRuntimeService()
                        .deleteProcessInstance(execution.getProcessInstanceId(), "Jobs are failed");
                });
        }
        return incidentEntity;
    }
}
