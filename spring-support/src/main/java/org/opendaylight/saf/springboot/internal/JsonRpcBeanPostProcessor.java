/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import com.google.common.base.Strings;
import com.google.gson.JsonParser;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.SubscriberSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.saf.governance.client.GovernanceClientImpl;
import org.opendaylight.saf.springboot.annotation.LookupService;
import org.opendaylight.saf.springboot.annotation.PublisherProxy;
import org.opendaylight.saf.springboot.annotation.RequesterProxy;
import org.opendaylight.saf.springboot.annotation.Responder;
import org.opendaylight.saf.springboot.annotation.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanPostProcessor} that takes care of JSONRPC annotations.
 *
 * <p>
 * Following annotations are supported:
 * </p>
 * <ul>
 * <li>{@link Responder} and {@link Subscriber} on bean instance</li>
 * <li>{@link RequesterProxy} and {@link PublisherProxy} on member field (non-static only)</li>
 * </ul>
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 28, 2019
 */
public class JsonRpcBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements BeanFactoryAware {
    private static final String INVALID_URI_ERR = "Invalid URI : ";
    private static final String GOVERNANCE_PROP = "${governance}";
    private static final JsonParser PARSER = new JsonParser();
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcBeanPostProcessor.class);
    private ConfigurableListableBeanFactory beanFactory;
    private TransportFactory transportFactory;
    private Optional<String> governanceEndpoint;

    public JsonRpcBeanPostProcessor(TransportFactory transportFactory) {
        this.transportFactory = Objects.requireNonNull(transportFactory);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
        governanceEndpoint = resolveProperty(GOVERNANCE_PROP);
    }

    private Optional<String> resolveProperty(String name) {
        try {
            return Optional.of(beanFactory.resolveEmbeddedValue(name));
        } catch (IllegalArgumentException e) {
            LOG.debug("Unable to resolve property '{}'", name, e);
            return Optional.empty();
        }
    }

    private static String formatFieldError(String error, Object bean, Field field) {
        return String.format("%s [type=%s,field=%s]", error, bean.getClass(), field.getName());
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        LOG.debug("postProcessAfterInitialization bean = {}, name = {}", bean, beanName);

        findAnnotatedFields(bean.getClass(), RequesterProxy.class).forEach(field -> {
            if (!AutoCloseable.class.isAssignableFrom(field.getType())) {
                throw new BeanCreationException(formatFieldError(
                        "Requester proxy type must be subclass of java.io.AutoCloseable", bean, field));
            }
            final RequesterProxy annotation = field.getAnnotation(RequesterProxy.class);
            String endpoint = annotation.value();
            if (Strings.isNullOrEmpty(endpoint)) {
                endpoint = lookupService(field);
            }
            createRequesterProxy(endpoint, beanName, field, bean);
        });

        findAnnotatedFields(bean.getClass(), PublisherProxy.class).forEach(field -> {
            if (!AutoCloseable.class.isAssignableFrom(field.getType())) {
                throw new BeanCreationException(formatFieldError(
                        "Publisher proxy type must be subclass of java.io.AutoCloseable", bean, field));
            }
            exposePublisherProxy(field.getAnnotation(PublisherProxy.class).value(), beanName, field, bean);
        });

        findAnnotationOnClass(bean.getClass(), Responder.class).forEach(annotation -> {
            if (!AutoCloseable.class.isAssignableFrom(bean.getClass())) {
                throw new BeanCreationException(
                        "Responder must be subclass of java.io.AutoCloseable [type=" + bean + "]");
            }
            exposeResponder(annotation.value(), bean, beanName);
        });

        findAnnotationOnClass(bean.getClass(), Subscriber.class).forEach(annotation -> {
            if (!AutoCloseable.class.isAssignableFrom(bean.getClass())) {
                throw new BeanCreationException(
                        "Subscriber must be subclass of java.io.AutoCloseable [type=" + bean + "]");
            }
            connectSubscriber(annotation.value(), bean, beanName);
        });

        return pvs;
    }

    private <T extends Annotation> Set<T> findAnnotationOnClass(final Class<?> clazz, Class<T> annotationType) {
        Class<?> targetClass = clazz;
        final Set<T> toProcess = new HashSet<>();
        do {
            toProcess.addAll(Arrays.asList(targetClass.getDeclaredAnnotationsByType(annotationType)));
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);
        return toProcess;
    }

    private Set<Field> findAnnotatedFields(final Class<?> clazz, final Class<? extends Annotation> annotationType) {
        Class<?> targetClass = clazz;
        final Set<Field> toProcess = new HashSet<>();
        do {
            toProcess.addAll(Arrays.asList(targetClass.getDeclaredFields())
                    .stream()
                    .filter(f -> f.getAnnotation(annotationType) != null)
                    .filter(f -> !Modifier.isStatic(f.getModifiers()))
                    .collect(Collectors.toSet()));
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);

        return toProcess;
    }

    private void createRequesterProxy(String uri, String parentBeanName, Field field, Object bean) {
        final String resolvedUri = beanFactory.resolveEmbeddedValue(uri);
        LOG.info("Connecting requester proxy to {} in type {}", resolvedUri, bean.getClass().getName());
        try {
            @SuppressWarnings("unchecked")
            final AutoCloseable proxy = transportFactory.endpointBuilder()
                    .requester()
                    .createProxy((Class<? extends AutoCloseable>) field.getType(), resolvedUri);

            final String beanName = generateBeanName(resolvedUri, "requester", bean);
            beanFactory.registerSingleton(beanName, proxy);
            beanFactory.registerDependentBean(parentBeanName, beanName);
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, proxy);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(INVALID_URI_ERR + resolvedUri, e);
        }
    }

    private void exposePublisherProxy(String uri, String parentBeanName, Field field, Object bean) {
        final String resolvedUri = beanFactory.resolveEmbeddedValue(uri);
        LOG.info("Exposing publisher proxy to {} in type {}", resolvedUri, bean.getClass().getName());
        try {
            @SuppressWarnings("unchecked")
            final AutoCloseable proxy = transportFactory.endpointBuilder()
                    .publisher()
                    .createProxy((Class<? extends AutoCloseable>) field.getType(), resolvedUri);
            final String beanName = generateBeanName(resolvedUri, "publisher", bean);
            beanFactory.registerSingleton(beanName, proxy);
            beanFactory.registerDependentBean(parentBeanName, beanName);
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, proxy);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(INVALID_URI_ERR + resolvedUri, e);
        }
    }

    private void exposeResponder(String uri, Object bean, String parentBeanName) {
        final String resolvedUri = beanFactory.resolveEmbeddedValue(uri);
        try {
            LOG.info("Exposing responder at {} for type {}", resolvedUri, bean.getClass().getName());
            final ResponderSession session = transportFactory.endpointBuilder()
                    .responder()
                    .create(resolvedUri, (AutoCloseable) bean);
            final String beanName = generateBeanName(resolvedUri, "responder", bean);
            beanFactory.registerSingleton(beanName, session);
            beanFactory.registerDependentBean(parentBeanName, beanName);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(INVALID_URI_ERR + resolvedUri, e);
        }
    }

    private void connectSubscriber(String uri, Object bean, String parentBeanName) {
        final String resolvedUri = beanFactory.resolveEmbeddedValue(uri);
        try {
            LOG.info("Connecting subscriber to {} for type {}", resolvedUri, bean.getClass().getName());
            final SubscriberSession session = transportFactory.endpointBuilder()
                    .subscriber()
                    .create(resolvedUri, (AutoCloseable) bean);
            final String beanName = generateBeanName(resolvedUri, "subscriber", bean);
            beanFactory.registerSingleton(beanName, session);
            beanFactory.registerDependentBean(parentBeanName, beanName);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(INVALID_URI_ERR + resolvedUri, e);
        }
    }

    private String lookupService(Field field) {
        if (!governanceEndpoint.isPresent()) {
            throw new IllegalStateException("Governance is not available. "
                    + "Make sure that environment variable GOVERNANCE is set to governance endpoint");
        }
        final LookupService annotation = Objects.requireNonNull(field.getAnnotation(LookupService.class),
                "Annotation LookupService is not present");
        try (GovernanceClientImpl client = new GovernanceClientImpl(transportFactory, governanceEndpoint.get())) {
            return client.governance(annotation.entity(), annotation.store(), PARSER.parse(annotation.path()))
                    .orElseThrow(() -> new IllegalStateException(String.format(
                            "Service lookup failed for entity %s and path %", annotation.entity(), annotation.path())));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String generateBeanName(String uri, String sessionType, Object bean) {
        return new StringBuilder().append(bean.getClass().getName())
                .append('_')
                .append(sessionType)
                .append(' ')
                .append(uri)
                .append(' ')
                .append(bean.hashCode())
                .toString();
    }
}
