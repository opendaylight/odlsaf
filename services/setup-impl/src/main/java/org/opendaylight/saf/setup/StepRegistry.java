/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StepRegistry {
    private final Map<String, ConfigStep> steps = new HashMap<>();

    public void register(ConfigStep step) {
        steps.put(step.name(), step);
    }

    public ConfigStep get(String name) {
        return Optional.ofNullable(steps.get(name))
                .orElseThrow(() -> new IllegalStateException("Step not found : " + name));
    }
}
