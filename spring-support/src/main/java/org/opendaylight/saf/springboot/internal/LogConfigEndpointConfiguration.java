/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URISyntaxException;
import java.util.Map;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.saf.springboot.annotation.EnableLogConfigEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JsonRpcAutoConfiguration.class)
public class LogConfigEndpointConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(LogConfigEndpointConfiguration.class);

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "findAnnotationOnBean can't "
            + "return null because choosen bean was found via getBeansWithAnnotation")
    @Bean
    public ResponderSession createResponder(ListableBeanFactory factory, TransportFactory transportFactory)
            throws URISyntaxException {
        final Map<String, Object> beans = factory.getBeansWithAnnotation(EnableLogConfigEndpoint.class);
        if (beans.size() != 1) {
            throw new IllegalArgumentException(
                    "Expected exactly one bean annotated with @EnableLogConfigEndpoint, but found " + beans);
        }
        final String uri = factory.findAnnotationOnBean(beans.keySet().iterator().next(), EnableLogConfigEndpoint.class)
                .value();
        LOG.info("Starting log configuration responder on {}", uri);
        return transportFactory.endpointBuilder().responder().create(uri, LogConfigResponder.INSTANCE);
    }
}
