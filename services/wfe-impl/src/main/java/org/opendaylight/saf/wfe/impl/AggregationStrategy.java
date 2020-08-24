/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Outbound notification aggregator. Current implementation uses size-based and time-based aggregation.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 27, 2019
 */
public class AggregationStrategy<T> implements TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(AggregationStrategy.class);
    private static final HashedWheelTimer TIMER = new HashedWheelTimer(
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("notification-aggregator-%d").build());
    private final Set<T> items = new LinkedHashSet<>();
    private final Consumer<List<T>> consumer;
    private final long intervalMillis;
    private final int batchSize;
    private Timeout timeout;

    public AggregationStrategy(Consumer<List<T>> consumer, int batchSize, long intervalMillis) {
        this.intervalMillis = intervalMillis;
        this.batchSize = batchSize;
        this.consumer = Objects.requireNonNull(consumer);
        resetTimer();
    }

    public synchronized void add(T item) {
        items.add(item);
        if (items.size() == batchSize) {
            dispatch();
            resetTimer();
        }
    }

    private void resetTimer() {
        Optional.ofNullable(timeout).ifPresent(Timeout::cancel);
        timeout = TIMER.newTimeout(this, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private synchronized void dispatch() {
        final List<T> copy = ImmutableList.copyOf(items);
        items.clear();
        LOG.debug("Sending notifications ");
        consumer.accept(copy);
    }

    @Override
    public void run(Timeout currentTimeout) throws Exception {
        if (!items.isEmpty()) {
            dispatch();
        }
        resetTimer();
    }
}
