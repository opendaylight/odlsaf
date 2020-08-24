/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.opendaylight.saf.setup")
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class Tool {
    public static void main(String... args) {
        SpringApplication.run(Tool.class, args);
    }
}
