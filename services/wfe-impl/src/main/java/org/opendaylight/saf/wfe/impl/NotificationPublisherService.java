/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import java.util.List;
import java.util.function.Consumer;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.extension.reactor.bus.CamundaSelector;
import org.camunda.bpm.extension.reactor.spring.listener.ReactorExecutionListener;
import org.opendaylight.saf.springboot.annotation.PublisherProxy;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SafWfeNotificationService;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.WorkflowStateChanged;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.workflows.Workflows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service that send aggregated job changes.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 27, 2019
 */
@Service
@ConditionalOnProperty(name = "publisher-endpoint")
@CamundaSelector()
public class NotificationPublisherService extends ReactorExecutionListener implements Consumer<List<Workflows>> {

    private final AggregationStrategy<Workflows> aggregator = new AggregationStrategy<>(this, 10, 1000L);

    @PublisherProxy("${publisher-endpoint}")
    private SafWfeNotificationService session;

    public void send(Workflows workflow) {
        aggregator.add(workflow);
    }

    @Override
    public void accept(List<Workflows> workflows) {
        session.workflowStateChanged(WorkflowStateChanged.builder().workflows(workflows).build());
    }

    @Override
    public void notify(DelegateExecution task) {
        // TODO : fetch process instance status
        send(Workflows.builder().jobId(task.getId()).build());
    }
}
