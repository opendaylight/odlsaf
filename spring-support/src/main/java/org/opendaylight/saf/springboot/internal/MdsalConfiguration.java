/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.binding.runtime.api.BindingRuntimeTypes;
import org.opendaylight.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.binding.runtime.spi.GeneratedClassLoadingStrategy;
import org.opendaylight.jsonrpc.binding.EmbeddedSchemaService;
import org.opendaylight.jsonrpc.impl.JsonConverter;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//TODO : Add support for persistence, eg. via flat files (json)
@Configuration
@EnableConfigurationProperties(MdsalConfigurationProperties.class)
public class MdsalConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalConfiguration.class);

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public ListeningExecutorService createExecutor() {
        return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonParser createJsonParser() {
        return new JsonParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public EffectiveModelContext createSchemaContext(MdsalConfigurationProperties properties)
            throws ReactorException, YangSyntaxErrorException, IOException {
        final Set<YangModuleInfo> modules = YangModuleUtil.filter(BindingReflections.loadModuleInfos(),
                properties.getSchemaModules());
        final CrossSourceStatementReactor.BuildAction reactor = RFC7950Reactors.defaultReactor().newBuild();
        for (YangModuleInfo module : modules) {
            reactor.addSource(YangStatementStreamSource.create(moduleToSource(module)));
        }
        return reactor.buildEffective();
    }

    private static YangTextSchemaSource moduleToSource(YangModuleInfo ymi) {
        final String name = YangModuleUtil.toSimpleName(ymi);
        LOG.info("Adding module to schema context : {}", name);
        return YangTextSchemaSource.delegateForByteSource(name + ".yang", ymi.getYangTextByteSource());
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonConverter createJsonConverter(EffectiveModelContext schemaContext) {
        return new JsonConverter(schemaContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public DOMSchemaService createSchemaService(EffectiveModelContext schemaContext) {
        return new EmbeddedSchemaService(schemaContext);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public DatastoreContext createDatastoreContext(DOMSchemaService schemaService) {
        return new DatastoreContext(schemaService);
    }

    @Bean(name = "DOMDataBroker", destroyMethod = "close")
    @ConditionalOnMissingBean
    public DOMDataBroker createDomDataBroker(DatastoreContext context, ListeningExecutorService executor) {
        return new SerializedDOMDataBroker(context.getStoreMap(), executor);
    }

    @Bean(name = "BindingRuntimeContext")
    public BindingRuntimeContext createBindingRuntimeContext(EffectiveModelContext schemaContext) {
        final BindingRuntimeTypes runtimeTypes = new BindingRuntimeTypes(schemaContext, Collections.emptyMap(),
                ImmutableBiMap.of(), ImmutableMultimap.of(), Collections.emptyMap());
        return DefaultBindingRuntimeContext.create(runtimeTypes,
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
    }

    @Bean(name = "BindingCodecContext")
    public BindingCodecContext createBindingCodecContext(BindingRuntimeContext runtimeContext) {
        return new BindingCodecContext(runtimeContext);
    }

    @Bean(name = "AdapterContext")
    public AdapterContext createAdapterContext(BindingCodecContext bindingCodecContext) {
        return new ConstantAdapterContext(bindingCodecContext);
    }

    @Bean(name = "DataBroker")
    @ConditionalOnMissingBean
    public DataBroker createDataBroker(AdapterContext adapterContext, DOMDataBroker domDataBroker) {
        return new BindingDOMDataBrokerAdapter(adapterContext, domDataBroker);
    }
}
