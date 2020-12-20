/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import org.opendaylight.saf.springboot.annotation.EnableHealthCheck;
import org.opendaylight.saf.springboot.annotation.EnableLogConfigEndpoint;
import org.opendaylight.saf.springboot.internal.MdsalConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@EnableLogConfigEndpoint
@EnableHealthCheck
@SpringBootApplication(scanBasePackages = {"org.opendaylight.saf.devicedb", "org.opendaylight.jsonrpc"})
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@Import(MdsalConfiguration.class)
public class Main {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Main.class, args);
        Thread.currentThread().join();
    }
}
