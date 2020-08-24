/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ResourceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForceAsyncBeforeParseListener extends AbstractBpmnParseListener {
    private static final Logger LOG = LoggerFactory.getLogger(ForceAsyncBeforeParseListener.class);

    @Override
    public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEvent) {
        if (scope instanceof ProcessDefinition) {
            LOG.debug("Forcing AsyncBefore on {} in {}", startEvent.getId(), ((ResourceDefinition) scope).getKey());
            startEvent.setAsyncBefore(true);
        }
    }
}