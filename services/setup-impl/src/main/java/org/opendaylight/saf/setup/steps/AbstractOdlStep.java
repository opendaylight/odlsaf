/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.setup.steps;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static org.opendaylight.saf.setup.SetupConstants.PROP_BODY;
import static org.opendaylight.saf.setup.SetupConstants.PROP_METHOD;
import static org.opendaylight.saf.setup.SetupConstants.PROP_PASSWORD;
import static org.opendaylight.saf.setup.SetupConstants.PROP_URI;
import static org.opendaylight.saf.setup.SetupConstants.PROP_USERNAME;

import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractOdlStep {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractOdlStep.class);

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    protected static int makeRequest(Map<String, String> properties) throws IOException {
        final URL url = new URL(properties.get(PROP_URI));
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(properties.computeIfAbsent(PROP_METHOD, m -> HttpMethod.GET.name()));
        LOG.info(">>> [{}] {}", properties.get(PROP_METHOD), url.toExternalForm());

        final String authHeader = Base64.getEncoder()
                .encodeToString((properties.computeIfAbsent(PROP_USERNAME, u -> "admin") + ":"
                        + properties.computeIfAbsent(PROP_PASSWORD, p -> "admin")).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty(AUTHORIZATION.toString(), "Basic " + authHeader);
        conn.setRequestProperty(CONTENT_TYPE.toString(), APPLICATION_JSON.toString());
        conn.setRequestProperty(ACCEPT.toString(), APPLICATION_JSON.toString());
        if (!HttpMethod.GET.name().equalsIgnoreCase(properties.get(PROP_METHOD)) && properties.get(PROP_BODY) != null) {
            conn.getOutputStream().write(properties.get(PROP_BODY).getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().flush();
        }

        ByteStreams.exhaust(conn.getInputStream());
        LOG.info("<<< HTTP {}", conn.getResponseCode());
        return conn.getResponseCode();
    }
}
