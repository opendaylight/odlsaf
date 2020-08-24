/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.extension.reactor.bus.CamundaSelector;
import org.camunda.bpm.extension.reactor.spring.listener.ReactorExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@link ExecutionListener} that will log every start/stop event of tasks within workflow.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Apr 8, 2020
 */
@Component
@CamundaSelector
public class LoggingActivityListener extends ReactorExecutionListener {
    private static final Set<String> EVENTS = ImmutableSet.of("start", "end");
    private static final Logger LOG = LoggerFactory.getLogger(LoggingActivityListener.class);

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if (execution instanceof ActivityExecution && EVENTS.contains(execution.getEventName())) {
            final ActivityExecution activity = (ActivityExecution) execution;
            LOG.info("[Event] : {}, [Activity] : {}", activity.getEventName(), activity.getActivityInstanceId());
        }
    }
}
