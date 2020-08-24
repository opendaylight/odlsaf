/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckServer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckServer.class);
    private final int port;
    private final EventLoopConfiguration config;
    private ChannelFuture future;
    private final ChannelHandler handler;

    public HealthCheckServer(final EventLoopConfiguration config, final int port, final ChannelHandler handler) {
        this.config = Objects.requireNonNull(config);
        this.port = port;
        this.handler = handler;
    }

    @PostConstruct
    public void init() {
        ServerBootstrap bootstrap = new ServerBootstrap().group(config.bossGroup(), config.workerGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(8192));
                        ch.pipeline().addLast(handler);
                    }
                });
        LOG.info("Starting health check server at http://0.0.0.0:{}", port);
        future = bootstrap.bind(port);
    }

    @Override
    @PreDestroy
    public void close() {
        future.cancel(true);
    }
}
