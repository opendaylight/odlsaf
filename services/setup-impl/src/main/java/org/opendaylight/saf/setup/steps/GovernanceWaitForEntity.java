/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup.steps;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonParser;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.saf.governance.client.GovernanceClientImpl;
import org.opendaylight.saf.setup.ConfigStep;

/**
 * Step to wait until mapping for given entity/path/store combination exists in governance.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 26, 2020
 */
public class GovernanceWaitForEntity implements ConfigStep {
    private static final String PROP_STORE = "store";
    private static final String PROP_ENTITY = "entity";
    private static final String PROP_PATH = "path";
    private static final String PROP_GOVERNANCE = "governance";
    private static final JsonParser PARSER = new JsonParser();
    private TransportFactory transportFactory;

    public GovernanceWaitForEntity(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public void doStep(Map<String, String> properties) {
        for (;;) {
            try (GovernanceClientImpl client = new GovernanceClientImpl(transportFactory,
                    properties.get(PROP_GOVERNANCE))) {
                final Optional<String> result = client.governance(properties.get(PROP_ENTITY),
                        properties.get(PROP_STORE), PARSER.parse(properties.get(PROP_PATH)));
                if (result.isPresent()) {
                    return;
                }
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
            Uninterruptibles.sleepUninterruptibly(5L, TimeUnit.SECONDS);
        }
    }

    @Override
    public String name() {
        return "governance_wait_for_entity";
    }
}
