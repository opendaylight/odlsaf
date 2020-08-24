/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.GetAllLoggersOutput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.GetLogLevelInput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.GetLogLevelOutput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.SafLoggingRpcService;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.SetDefaultLogLevelInput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.SetDefaultLogLevelOutput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.SetLogLevelInput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.SetLogLevelOutput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.SetOpentracingLoggingInput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.SetOpentracingLoggingOutput;
import org.opendaylight.saf.core.api.saf_logging.gen.rev20200316.loggerLevelList.LoggerLevelList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SafLoggingRpcService} that uses reflection to access Logback implementation. Reason
 * for that is to prevent classpath pollution.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Oct 22, 2019
 */
public class LogConfigResponder implements SafLoggingRpcService {
    public static final LogConfigResponder INSTANCE = new LogConfigResponder();

    private static final Logger LOG = LoggerFactory.getLogger(LogConfigResponder.class);
    private static boolean available;
    private static Method valueOfMethod;
    private static Method setLevelMethod;
    private static Method getLevelMethod;
    private static Method loggerListMethod;

    static {
        try {
            final Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            final Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
            valueOfMethod = levelClass.getMethod("valueOf", String.class);
            setLevelMethod = loggerClass.getMethod("setLevel", levelClass);
            getLevelMethod = loggerClass.getMethod("getLevel", (Class<?>[]) null);
            loggerListMethod = LoggerFactory.getILoggerFactory()
                    .getClass()
                    .getMethod("getLoggerList", (Class<?>[]) null);
            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            LOG.error("Unable to initialize log configuration service, perhaps SLF4J implementation is not Logback?",
                    e);
            available = false;
        }
    }

    @Override
    public SetLogLevelOutput setLogLevel(SetLogLevelInput input) {
        ensureAvailability();
        setLogLevelValue(input.getLoggerName(), input.getLogLevel());
        return SetLogLevelOutput.builder().status(true).build();
    }

    @Override
    public GetAllLoggersOutput getAllLoggers() {
        ensureAvailability();
        return GetAllLoggersOutput.builder()
                .loggerLevelList(getLoggerList().stream()
                        .filter(LogConfigResponder::isLevelConfigured)
                        .map(logger -> LoggerLevelList.builder()
                                .loggerName(logger.getName())
                                .logLevel(String.valueOf(getLevelUnchecked(logger)))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public SetDefaultLogLevelOutput setDefaultLogLevel(SetDefaultLogLevelInput input) {
        ensureAvailability();
        setLogLevelValue(Logger.ROOT_LOGGER_NAME, input.getLogLevel());
        return SetDefaultLogLevelOutput.builder().status(true).build();
    }

    @Override
    public GetLogLevelOutput getLogLevel(GetLogLevelInput input) {
        ensureAvailability();
        final String level = Optional.ofNullable(getLogLevelValue(input.getLoggerName()))
                .orElse(getLogLevelValue(Logger.ROOT_LOGGER_NAME));
        return GetLogLevelOutput.builder().logLevel(level).build();
    }

    @Override
    public SetOpentracingLoggingOutput setOpentracingLogging(SetOpentracingLoggingInput input) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @SneakyThrows({ IllegalAccessException.class, InvocationTargetException.class })
    private static void setLogLevelValue(String loggerName, String loggerValue) {
        LOG.debug("Changing log level of logger '{}' to '{}'", loggerName, loggerValue);
        Objects.requireNonNull(loggerValue, "Level must be specified");
        final Object level = valueOfMethod.invoke(null, loggerValue.toUpperCase(Locale.US));
        final Logger loggerObject = LoggerFactory.getLogger(loggerName);
        setLevelMethod.invoke(loggerObject, level);
    }

    @SneakyThrows({ IllegalAccessException.class, InvocationTargetException.class })
    private static String getLogLevelValue(String loggerName) {
        return String.valueOf(getLevelMethod.invoke(LoggerFactory.getLogger(loggerName)));
    }

    @SneakyThrows({ IllegalAccessException.class, InvocationTargetException.class })
    private static Object getLevelUnchecked(Object loggerObject) {
        return getLevelMethod.invoke(loggerObject);
    }

    @SneakyThrows({ IllegalAccessException.class, InvocationTargetException.class })
    @SuppressWarnings("unchecked")
    private static List<Logger> getLoggerList() {
        return (List<Logger>) loggerListMethod.invoke(LoggerFactory.getILoggerFactory());
    }

    private static boolean isLevelConfigured(Logger logger) {
        return getLevelUnchecked(logger) != null;
    }

    private static void ensureAvailability() {
        if (!available) {
            throw new UnsupportedOperationException("Service is not available");
        }
    }
}
