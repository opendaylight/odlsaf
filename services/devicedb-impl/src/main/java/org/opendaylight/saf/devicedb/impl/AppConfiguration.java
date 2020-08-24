/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import java.net.URISyntaxException;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.impl.JsonRpcPathCodec;
import org.opendaylight.saf.governance.client.GovernanceClient;
import org.opendaylight.saf.governance.client.GovernanceClientImpl;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {
    @Bean
    public GovernanceClient governanceControlClient(TransportFactory transportFactory,
            @Value("${governance}") String endpoint) throws URISyntaxException {
        return new GovernanceClientImpl(transportFactory, endpoint);
    }

    @Bean
    public JsonRpcPathCodec pathCodec(SchemaContext schemaContext) {
        return JsonRpcPathCodec.create(schemaContext);
    }
}
