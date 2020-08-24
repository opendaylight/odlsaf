/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import org.camunda.bpm.extension.reactor.spring.EnableCamundaEventBus;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.opendaylight.saf.springboot.annotation.EnableHealthCheck;
import org.opendaylight.saf.springboot.annotation.EnableLogConfigEndpoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * @author spichandi
 *
 */
@SpringBootApplication(scanBasePackages = "org.opendaylight.saf.wfe")
@EnableProcessApplication("wfe")
@EnableCamundaEventBus
@EnableHealthCheck
@EnableLogConfigEndpoint
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class WFEApplication {
    public static void main(String... args) {
        SpringApplication.run(WFEApplication.class, args);
    }
}
