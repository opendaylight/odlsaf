/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.io.Files;
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
        props.put("endpoint", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort() + "?timeout=1000000000");
        props.put("data-endpoint", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort() + "?timeout=1000000000");
        props.put("devicedb-endpoint", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort());
        props.put("napalm", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort());
        props.put("workspace", Files.createTempDir().getAbsolutePath());
        props.put("restconf", "http://127.0.0.1:8181");
        props.put("restconf-username", "admin");
        props.put("restconf-password", "admin");
        props.put("publisher-endpoint", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort());
        final PropertySourcesPlaceholderConfigurer cfg = new PropertySourcesPlaceholderConfigurer();
        cfg.setProperties(props);
        return cfg;
    }
}
