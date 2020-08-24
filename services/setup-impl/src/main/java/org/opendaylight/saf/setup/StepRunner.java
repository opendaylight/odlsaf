/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup;

import java.util.List;
import org.opendaylight.saf.setup.Config.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepRunner implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(StepRunner.class);
    private List<Step> steps;
    private StepRegistry registry;

    public StepRunner(Config config, StepRegistry registry) {
        steps = config.getSteps();
        this.registry = registry;
    }

    public void run() {
        for (Step step : steps) {
            LOG.info("Running step '{}'", step.getName());
            final ConfigStep configStep = registry.get(step.getName());
            configStep.doStep(step.getProperties());
        }
    }
}
