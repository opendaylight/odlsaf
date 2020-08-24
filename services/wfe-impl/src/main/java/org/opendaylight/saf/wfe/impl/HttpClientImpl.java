/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpMethod;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.saf.opentracing.support.TracingConstants;
import org.opendaylight.saf.opentracing.support.TracingSupport;
import org.opendaylight.saf.wfe.impl.model.HttpClient;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.util.DelegateUtils;
import org.opendaylight.saf.wfe.util.HttpURLConnectionInjectAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link HttpClient} using {@link HttpURLConnection}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 5, 2020
 */
@Component
public class HttpClientImpl implements HttpClient {
    public static final Set<String> OUTPUT_METHODS = ImmutableSet.<String>builder()
            .add(HttpMethod.POST.name())
            .add(HttpMethod.PUT.name())
            .add(HttpMethod.PATCH.name())
            .build();

    @Autowired
    private Gson gson;

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public HttpResponse call(String method, String uri, Integer connectionTimeout, Integer readTimeout,
            Map<String, String> headers, String body) throws IOException {

        final boolean hasOutput = OUTPUT_METHODS.contains(method) && body != null;
        final URL url = new URL(uri);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // method
        connection.setRequestMethod(method);

        // timeouts
        if (connectionTimeout != null) {
            connection.setConnectTimeout(connectionTimeout);
        }
        if (readTimeout != null) {
            connection.setReadTimeout(readTimeout);
        }

        connection.setInstanceFollowRedirects(false);
        headers.entrySet().forEach(e -> connection.setRequestProperty(e.getKey(), e.getValue()));

        if (TracingSupport.isTracing()) {
            final Tracer tracer = TracingSupport.getTracer();
            final Span span = tracer.buildSpan(method).start();
            span.setTag(TracingConstants.TAG_HTTP_URL, uri);
            span.setTag(TracingConstants.TAG_HTTP_METHOD, method);
            span.setTag(TracingConstants.TAG_COMPONENT, TracingConstants.VALUE_NET_HTTP);
            tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
                    HttpURLConnectionInjectAdapter.create(connection));
            try (Scope scope = tracer.scopeManager().activate(span)) {
                return doRequest(headers, body, hasOutput, connection);
            } catch (Exception e) {
                Tags.ERROR.set(span, true);
                span.log(ImmutableMap.<String, Object>of(Fields.EVENT, TracingConstants.FIELD_ERROR,
                        Fields.ERROR_OBJECT, e, Fields.MESSAGE, String.valueOf(e.getMessage())));
                throw e;
            } finally {
                span.finish();
            }
        } else {
            return doRequest(headers, body, hasOutput, connection);
        }
    }

    private HttpResponse doRequest(Map<String, String> headers, String body, final boolean hasOutput,
            final HttpURLConnection connection) throws IOException {
        if (hasOutput) {
            final String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
            final String toWrite;
            if (MediaType.FORM_DATA.toString().equals(contentType)) {
                final Map<String, String> formData = gson.fromJson(body, DelegateUtils.MAP_TYPE);
                toWrite = formData.entrySet()
                        .stream()
                        .map(HttpClientImpl::mapEntryUnchecked)
                        .collect(Collectors.joining("&"));
            } else {
                toWrite = body;
            }
            connection.setDoOutput(true);
            connection.getOutputStream().write(toWrite.getBytes(StandardCharsets.UTF_8));
            connection.getOutputStream().flush();
        }

        connection.connect();
        final int code = connection.getResponseCode();
        final InputStream is = (code < 400) ? connection.getInputStream() : connection.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                Optional.ofNullable(is).orElse(DelegateUtils.NullInputStream.INSTANCE), StandardCharsets.UTF_8));
        StringWriter sb = new StringWriter();
        CharStreams.copy(br, sb);
        final String response = sb.toString();
        connection.disconnect();
        return new HttpResponse(code, response, connection.getHeaderFields());
    }

    private static String mapEntryUnchecked(Entry<String, String> entry) {
        try {
            return String.format("%s=%s", URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}