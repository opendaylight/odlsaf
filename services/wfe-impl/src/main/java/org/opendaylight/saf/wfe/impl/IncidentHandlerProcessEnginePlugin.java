/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import java.util.Collections;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Custom plugin for Incident handler.
 *
 * @author <a href="mailto:spichandi@luminanetworks.com">Sowmiya Pichandi</a>
 * @since Dec 18, 2019
 */
@Component
public class IncidentHandlerProcessEnginePlugin extends AbstractProcessEnginePlugin {
    private static final Logger LOG = LoggerFactory.getLogger(IncidentHandlerProcessEnginePlugin.class);

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        LOG.debug("Adding Failed Job Incident Handler");
        processEngineConfiguration
                .setCustomIncidentHandlers(Collections.singletonList(new FailedJobIncidentHandler("failedjob")));
    }
}