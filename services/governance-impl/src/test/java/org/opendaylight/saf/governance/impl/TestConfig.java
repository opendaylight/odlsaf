/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import com.google.common.io.Files;
import java.util.Properties;
import org.opendaylight.jsonrpc.bus.messagelib.TestHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class TestConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer getTestProperties() {
        final Properties props = new Properties();
        props.put("endpoint", "zmq://127.0.0.1:" + TestHelper.getFreeTcpPort());
        props.put("yang-root", Files.createTempDir().getAbsolutePath());
        final PropertySourcesPlaceholderConfigurer cfg = new PropertySourcesPlaceholderConfigurer();
        cfg.setProperties(props);
        return cfg;
    }
}
