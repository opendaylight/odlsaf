/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(JsonRpcAutoConfiguration.class)
@Configuration
@EnableConfigurationProperties(JsonRpcConfigurationProperties.class)
public class HealthCheckConfiguration {

    @Bean
    public HealthCheckServiceImpl createHealthCheckService() {
        return new HealthCheckServiceImpl();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public HealthCheckServer createHealthCheckServer(JsonRpcConfigurationProperties properties,
            EventLoopConfiguration config, HealthCheckServiceImpl checkService) {
        return new HealthCheckServer(config, properties.getHealthCheckPort(), checkService);
    }
}
