/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup;

import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.saf.setup.steps.GovernanceWaitForEntity;
import org.opendaylight.saf.springboot.internal.JsonRpcAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link Configuration} to create {@link GovernanceWaitForEntity} only if property "governance" is specified.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 27, 2020
 */
@Configuration
@Import(JsonRpcAutoConfiguration.class)
@ConditionalOnProperty("governance")
public class GovernanceConfiguration {
    public GovernanceWaitForEntity setupGovernanceStep(TransportFactory transportFactory, StepRegistry registry) {
        final GovernanceWaitForEntity step = new GovernanceWaitForEntity(transportFactory);
        registry.register(step);
        return step;
    }
}
