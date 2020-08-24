/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.opentracing.support;

/**
 * Common tracing constants.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 5, 2020
 */
public final class TracingConstants {
    public static final String FIELD_ERROR = "error";
    public static final String VALUE_NET_HTTP = "net/http";
    public static final String TAG_COMPONENT = "component";
    public static final String TAG_HTTP_METHOD = "http.method";
    public static final String TAG_HTTP_URL = "http.url";
    public static final String TAG_HTTP_QUERY = "http.query";
    public static final String TAG_HTTP_CLIENT = "http.client";
    public static final String TAG_HTTP_STATUS_CODE = "http.status_code";

    private TracingConstants() {
        // NOOP
    }
}
