/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.collect.ImmutableMap;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.camunda.bpm.engine.impl.delegate.DefaultDelegateInterceptor;
import org.camunda.bpm.engine.impl.delegate.DelegateInvocation;
import org.camunda.bpm.engine.impl.interceptor.DelegateInterceptor;
import org.opendaylight.saf.opentracing.support.TracingSupport;
import org.opendaylight.saf.wfe.util.DelegateConstants;
import org.opendaylight.saf.wfe.util.VariableScopeExtractAdapter;

/**
 * {@link DelegateInterceptor} used when opentracing is active.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 17, 2020
 */
@SuppressWarnings("checkstyle:IllegalCatch")
public class TracingDelegateInterceptor extends DefaultDelegateInterceptor {
    @Override
    public void handleInvocation(DelegateInvocation invocation) throws Exception {
        if (TracingSupport.isTracing() && invocation.getContextExecution() != null) {

            final Tracer tracer = TracingSupport.getTracer();
            final SpanContext parent = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VariableScopeExtractAdapter(invocation.getContextExecution()));
            final Span span = tracer
                    .buildSpan(String.valueOf(invocation.getContextExecution()
                            .getVariable(DelegateConstants.WORKFLOW_NAME)))
                    .asChildOf(parent)
                    .start();
            try (Scope scope = tracer.scopeManager().activate(span)) {
                super.handleInvocation(invocation);
            } catch (Exception e) {
                Tags.ERROR.set(span, true);
                span.log(ImmutableMap.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, e, Fields.MESSAGE,
                        String.valueOf(e.getMessage())));
                throw e;
            } finally {
                span.finish();
            }
        } else {
            super.handleInvocation(invocation);
        }
    }
}
