/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jsonrpc")
@Data
public class JsonRpcConfigurationProperties {
    private int workerThreads = 8;

    private int bossThreads = 2;

    private int handlerThreads = 8;

    private int healthCheckPort = 8000;
}
