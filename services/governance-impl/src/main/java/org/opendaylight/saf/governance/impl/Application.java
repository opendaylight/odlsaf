/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import org.opendaylight.saf.springboot.annotation.EnableHealthCheck;
import org.opendaylight.saf.springboot.annotation.EnableLogConfigEndpoint;
import org.opendaylight.saf.springboot.internal.JsonRpcAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableHealthCheck
@EnableLogConfigEndpoint
@Import({ JsonRpcAutoConfiguration.class })
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class Application {
    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }
}
