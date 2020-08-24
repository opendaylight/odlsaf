/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.opendaylight.jsonrpc.binding.EmbeddedBusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.messagelib.AbstractTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.bus.spi.DefaultEventLoopConfiguration;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(TransportFactory.class)
@EnableConfigurationProperties(JsonRpcConfigurationProperties.class)
public class JsonRpcAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public TransportFactory createTransportFactory(EventLoopConfiguration config) {
        return new CustomTransportFactory(new EmbeddedBusSessionFactoryProvider(config));
    }

    @Bean
    @ConditionalOnMissingBean
    public EventLoopConfiguration createConfiguration(JsonRpcConfigurationProperties properties) {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        final EventLoopGroup workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());
        final EventExecutorGroup handlerGroup = new DefaultEventExecutorGroup(properties.getHandlerThreads());
        return new DefaultEventLoopConfiguration(bossGroup, workerGroup, handlerGroup);
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcBeanPostProcessor createAnnotationInjector(ConfigurableBeanFactory beanFactory,
            TransportFactory transportFactory) {
        final JsonRpcBeanPostProcessor processor = new JsonRpcBeanPostProcessor(transportFactory);
        beanFactory.addBeanPostProcessor(processor);
        return processor;
    }

    private static class CustomTransportFactory extends AbstractTransportFactory {
        protected CustomTransportFactory(BusSessionFactoryProvider provider) {
            super(provider);
        }
    }
}
