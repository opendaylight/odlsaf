/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.plastic.impl;

import org.opendaylight.saf.springboot.annotation.EnableHealthCheck;
import org.opendaylight.saf.springboot.annotation.EnableLogConfigEndpoint;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * @see PlasticServiceAdapter
 */
@SpringBootApplication(scanBasePackages = "org.opendaylight.saf.plastic.impl")
@EnableHealthCheck
@EnableLogConfigEndpoint
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class Main implements CommandLineRunner {
    public static void main(String... args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // keep application running after all components are initialized
        Thread.currentThread().join();
    }
}
