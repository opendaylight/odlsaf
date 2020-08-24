/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.opendaylight.saf.opentracing.support.TracingSupport;
import org.springframework.stereotype.Component;

@Component
public class OpentracingSupportPlugin extends AbstractProcessEnginePlugin {
    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (TracingSupport.isTracing()) {
            processEngineConfiguration.setDelegateInterceptor(new TracingDelegateInterceptor());
        }
        super.preInit(processEngineConfiguration);
    }
}
