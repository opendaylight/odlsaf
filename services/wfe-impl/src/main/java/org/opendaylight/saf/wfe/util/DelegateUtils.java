/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;

public final class DelegateUtils {
    public static final class NullInputStream extends InputStream {
        public static final NullInputStream INSTANCE = new NullInputStream();

        private NullInputStream() {
        }

        @Override
        public int read() {
            return -1;
        }

        @Override
        public int available() {
            return 0;
        }
    }

    private static final Escaper URI_ESCAPER = UrlEscapers.urlPathSegmentEscaper();
    private static final Pattern CURLY_BR_RE = Pattern.compile("\\{(\\S[^\\}]*)\\}");
    @SuppressWarnings("serial")
    public static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private DelegateUtils() {
        // utility class constructor
    }

    /**
     * Escape leaf/list entry to be RFC-2396 compliant.
     *
     * @param raw unescaped path
     * @return escaped path
     */
    public static String getEncodedPath(final String raw) {
        final Matcher m = CURLY_BR_RE.matcher(raw);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, URI_ESCAPER.escape(m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Extract device name from given YANG path. This method assumes that path is valid and will not do any checks.
     *
     * @param yangPath path to extract device name from
     * @return device name
     */
    public static String extractDeviceName(String yangPath) {
        final String path = yangPath.substring(DelegateConstants.JSONRPC_BASE_URI.length());
        if (path.indexOf('/') != -1) {
            return path.substring(0, path.indexOf('/'));
        }
        return path;
    }

    /**
     * Helper which wait (at most) for given period to proxy become ready for use.
     *
     * @param factory {@link TransportFactory} used to create proxy
     * @param proxy actual proxy
     * @param milliseconds interval to wait (at most)
     */
    public static void awaitForProxy(TransportFactory factory, AutoCloseable proxy, long milliseconds) {
        final long future = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < future) {
            if (factory.isClientConnected(proxy)) {
                return;
            }
            Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Remove array-element from entire JSON tree.
     *
     * @param data unprocessed {@link JsonElement}.
     * @return fixed {@link JsonElement}.
     */
    public static JsonElement removeArrayElement(JsonElement data) {
        if (data.isJsonObject()) {
            for (Entry<String, JsonElement> entry : data.getAsJsonObject().entrySet()) {
                if ("array-element".equals(entry.getKey())) {
                    final JsonElement inner = removeArrayElement(entry.getValue());
                    if (inner.isJsonArray()) {
                        return inner;
                    } else {
                        final JsonArray array = new JsonArray(1);
                        array.add(inner);
                        return array;
                    }
                }
                if (entry.getValue().isJsonObject()) {
                    entry.setValue(removeArrayElement(entry.getValue()));
                }
            }
        }
        if (data.isJsonArray()) {
            for (int i = 0; i < data.getAsJsonArray().size(); i++) {
                data.getAsJsonArray().set(i, removeArrayElement(data.getAsJsonArray().get(i)));
            }
        }
        return data;
    }

    public static boolean isHttpOk(int code) {
        return code < 400;
    }
}
