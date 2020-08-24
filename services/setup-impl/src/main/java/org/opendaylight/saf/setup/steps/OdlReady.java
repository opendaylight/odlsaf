/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup.steps;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.saf.setup.ConfigStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check if ODL instance is ready by making query to "/diagstatus" endpoint.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 26, 2020
 */
public final class OdlReady extends AbstractOdlStep implements ConfigStep {
    private static final Logger LOG = LoggerFactory.getLogger(OdlReady.class);
    public static final OdlReady INSTANCE = new OdlReady();

    private OdlReady() {
        // NOOP
    }

    @Override
    public void doStep(Map<String, String> properties) {
        properties.put("uri", "/diagstatus");
        int code = -1;
        for (;;) {
            try {
                code = makeRequest(properties);
                if (code < 400) {
                    return;
                }
            } catch (IOException e) {
                LOG.error("Ready check failed", e);
            }
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
        }
    }

    @Override
    public String name() {
        return "odl_ready";
    }
}
