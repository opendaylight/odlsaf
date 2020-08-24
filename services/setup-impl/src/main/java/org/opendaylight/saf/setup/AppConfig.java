/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup;

import org.opendaylight.saf.setup.steps.K8sWaitPod;
import org.opendaylight.saf.setup.steps.OdlReady;
import org.opendaylight.saf.setup.steps.OdlRestconf;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public StepRegistry createStepRegistry() {
        final StepRegistry reg = new StepRegistry();
        reg.register(K8sWaitPod.INSTANCE);
        reg.register(OdlReady.INSTANCE);
        reg.register(OdlRestconf.INSTANCE);
        return reg;
    }
}
