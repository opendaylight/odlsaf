/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.opentracing.restconf;

import static org.opendaylight.saf.opentracing.support.TracingConstants.FIELD_ERROR;
import static org.opendaylight.saf.opentracing.support.TracingConstants.TAG_COMPONENT;
import static org.opendaylight.saf.opentracing.support.TracingConstants.TAG_HTTP_CLIENT;
import static org.opendaylight.saf.opentracing.support.TracingConstants.TAG_HTTP_METHOD;
import static org.opendaylight.saf.opentracing.support.TracingConstants.TAG_HTTP_QUERY;
import static org.opendaylight.saf.opentracing.support.TracingConstants.TAG_HTTP_STATUS_CODE;
import static org.opendaylight.saf.opentracing.support.TracingConstants.TAG_HTTP_URL;
import static org.opendaylight.saf.opentracing.support.TracingConstants.VALUE_NET_HTTP;

import com.google.common.collect.ImmutableMap;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.web.servlet.filter.HttpServletRequestExtractAdapter;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.opendaylight.saf.opentracing.support.TracingSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Filter} that wraps every {@link Servlet} invocation in opentracing {@link Span}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 3, 2020
 */
public class RestconfFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(RestconfFilter.class);

    static {
        if (!TracingSupport.isTracing()) {
            LOG.info("RESTConf tracing is DISABLED");
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (TracingSupport.isTracing() || !(request instanceof HttpServletRequest)
                || !(response instanceof HttpServletResponse)) {
            final Tracer tracer = TracingSupport.getTracer();
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            final HttpServletResponse httpResponse = (HttpServletResponse) response;
            final SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                    new HttpServletRequestExtractAdapter(httpRequest));

            final Span span = tracer.buildSpan(httpRequest.getMethod())
                    .asChildOf(extractedContext)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                    .start();

            try (Scope scope = tracer.scopeManager().activate(span)) {
                attachRequestData(httpRequest, span);
                chain.doFilter(request, response);
            } catch (Exception e) {
                Tags.ERROR.set(span, true);
                span.log(ImmutableMap.<String, Object>of(Fields.EVENT, FIELD_ERROR, Fields.ERROR_OBJECT, e,
                        Fields.MESSAGE, e.getMessage()));
                throw e;
            } finally {
                attachResponseData(httpResponse, span);
                span.finish();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void attachRequestData(HttpServletRequest request, Span span) {
        span.setTag(TAG_HTTP_CLIENT, request.getRemoteAddr());
        span.setTag(TAG_HTTP_URL, request.getRequestURI());
        span.setTag(TAG_HTTP_METHOD, request.getMethod());
        span.setTag(TAG_HTTP_QUERY, request.getQueryString());
        span.setTag(TAG_COMPONENT, VALUE_NET_HTTP);
    }

    private void attachResponseData(HttpServletResponse response, Span span) {
        span.setTag(TAG_HTTP_STATUS_CODE, response.getStatus());
    }

    @Override
    public void destroy() {
        // NOOP
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        // NOOP
    }
}
