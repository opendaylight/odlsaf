/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.opentracing.propagation.TextMap;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Inject adapter for {@link HttpURLConnection}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 5, 2020
 */
public final class HttpURLConnectionInjectAdapter implements TextMap {
    private final HttpURLConnection connection;

    private HttpURLConnectionInjectAdapter(HttpURLConnection connection) {
        this.connection = connection;
    }

    public static HttpURLConnectionInjectAdapter create(@NonNull HttpURLConnection connection) {
        return new HttpURLConnectionInjectAdapter(Objects.requireNonNull(connection));
    }

    @Override
    public void put(String key, String value) {
        connection.setRequestProperty(key, value);
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("This class should be used only with tracer#inject()");
    }
}
