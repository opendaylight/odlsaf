/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.saf.springboot.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Implementation of {@link HealthCheckService} that also handles inbound HTTP check requests and listen for context
 * refresh event.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Sep 22, 2019
 */
@Sharable
public class HealthCheckServiceImpl extends SimpleChannelInboundHandler<FullHttpRequest>
        implements HealthCheckService, ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckServiceImpl.class);
    private final AtomicBoolean flag = new AtomicBoolean(false);
    private final AtomicReference<String> extraInfo = new AtomicReference<>(null);

    @Override
    public void setStatus(boolean healthy, String additionalInfo) {
        flag.set(healthy);
        extraInfo.set(additionalInfo);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        final JsonObject data = new JsonObject();
        final ByteBuf buffer = Unpooled.buffer();
        data.add("health", new JsonPrimitive(flag.get()));
        data.add("info", new JsonPrimitive(Optional.ofNullable(extraInfo.get()).orElse("N/A")));
        buffer.writeCharSequence(data.toString(), StandardCharsets.UTF_8);
        ctx.channel()
                .writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        flag.get() ? HttpResponseStatus.OK : HttpResponseStatus.SERVICE_UNAVAILABLE, buffer))
                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOG.info("Marking this service as ready");
        flag.set(true);
    }
}
