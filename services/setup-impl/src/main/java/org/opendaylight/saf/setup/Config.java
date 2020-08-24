/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Config {
    private Map<String, String> tokens;
    private List<Step> steps;

    @AllArgsConstructor
    @Data
    public static class Step {
        private String name;
        private Map<String, String> properties;
    }
}
