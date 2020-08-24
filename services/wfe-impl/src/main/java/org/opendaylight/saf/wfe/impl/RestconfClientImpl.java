/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.annotation.PostConstruct;
import org.opendaylight.saf.wfe.impl.model.HttpClient;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.impl.model.RestconfClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestconfClientImpl implements RestconfClient {
    private static final Logger LOG = LoggerFactory.getLogger(RestconfClientImpl.class);
    @Value("${restconf-username}")
    private String username;
    @Value("${restconf-password}")
    private String password;
    @Value("${restconf}")
    private String endpoint;
    @Autowired
    private HttpClient client;

    @PostConstruct
    public void init() {
        LOG.info("RESTConf endpoint set to {}", endpoint);
    }

    @Override
    public HttpResponse call(String method, String uri, String body) throws IOException {
        final String authHeader = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        return client.call(method, endpoint + '/' + uri, null, null,
                ImmutableMap.<String, String>builder()
                        .put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + authHeader)
                        .put(HttpHeaderNames.ACCEPT.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                        .put(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                        .build(),
                body);
    }
}
