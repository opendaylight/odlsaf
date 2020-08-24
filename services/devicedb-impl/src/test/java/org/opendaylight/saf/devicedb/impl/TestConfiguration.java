/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import java.util.Properties;
import org.opendaylight.jsonrpc.bus.messagelib.TestHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class TestConfiguration {
    @Bean
    public static PropertySourcesPlaceholderConfigurer getTestProperties() {
        final Properties props = new Properties();
        props.put("operations", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort() + "?timeout=10000");
        props.put("governance", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort() + "?timeout=10000");
        props.put("napalm", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort() + "?timeout=10000");
        props.put("cliengine", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort() + "?timeout=10000");
        props.put("advertise-operations", props.get("operations"));
        final PropertySourcesPlaceholderConfigurer cfg = new PropertySourcesPlaceholderConfigurer();
        cfg.setProperties(props);
        return cfg;
    }
}
