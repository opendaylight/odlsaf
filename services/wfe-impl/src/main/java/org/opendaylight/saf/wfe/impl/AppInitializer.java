/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.gson.JsonParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppInitializer {
    @Bean
    public JsonParser createJsonParser() {
        return new JsonParser();
    }

    @Bean
    public FixedDefaultJobExecutor createJobExecutor() {
        return new FixedDefaultJobExecutor();
    }
}
