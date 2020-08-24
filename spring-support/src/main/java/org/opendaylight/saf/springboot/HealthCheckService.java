/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot;

/**
 * API to interact with health check reporter.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Oct 21, 2019
 */
public interface HealthCheckService {
    /**
     * Set current status.
     *
     * @param healthy flag to indicate health status
     * @param additionalInfo additional info to report
     */
    void setStatus(boolean healthy, String additionalInfo);

    /**
     * Set current health flag.
     *
     * @param healthy flag to indicate health status
     */
    default void setHealthy(boolean healthy) {
        setStatus(healthy, null);
    }
}
