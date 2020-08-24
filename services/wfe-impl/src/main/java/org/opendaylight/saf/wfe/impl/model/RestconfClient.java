/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import java.io.IOException;

/**
 * Simple REST client.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 21, 2019
 */
public interface RestconfClient {
    /**
     * Invoke REST method.
     *
     * @param method HTTP method to call (eg. GET, POST , ...)
     * @param uri uri fragment without hostname and port
     * @param body optional body
     * @return {@link HttpResponse}
     * @throws IOException when some I/O problem occurr
     */
    HttpResponse call(String method, String uri, String body) throws IOException;
}