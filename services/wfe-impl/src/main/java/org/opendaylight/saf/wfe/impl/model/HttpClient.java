/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import java.io.IOException;
import java.util.Map;

/**
 * Simple HTTP client.
 *
 * @author <a href="mailto:pchopra@luminanetworks.com">Priyanka Chopra</a>
 * @since May 21, 2019
 */
public interface HttpClient {
    /**
     * Invoke HTTP method.
     *
     * @param method HTTP method to call (eg. GET, POST , ...)
     * @param uri Full URL
     * @param body optional body
     * @param connectionTimeout connection timeout (can be null)
     * @param readTimeout read timeout (can be null)
     * @param headers HTTP request headers (can be null)
     * @return {@link HttpResponse}
     * @throws IOException when some I/O problem occur
     */
    HttpResponse call(String method, String uri, Integer connectionTimeout, Integer readTimeout,
            Map<String, String> headers, String body) throws IOException;
}