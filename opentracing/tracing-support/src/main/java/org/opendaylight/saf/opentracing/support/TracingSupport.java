/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.opentracing.support;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import java.util.Optional;

public final class TracingSupport {
    private static final boolean IS_TRACING;

    private TracingSupport() {
        // NOOP
    }

    static {
        IS_TRACING = Optional.ofNullable(System.getenv(Configuration.JAEGER_AGENT_HOST)).isPresent();
    }

    public static boolean isTracing() {
        return IS_TRACING;
    }

    public static Tracer getTracer() {
        return GlobalTracer.get();
    }
}
