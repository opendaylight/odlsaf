/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

import com.google.common.collect.ImmutableSet;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMap;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.eclipse.jdt.annotation.NonNull;

/**
 * {@link VariableScope} adapter for {@link Tracer#extract(io.opentracing.propagation.Format, Object)}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 17, 2020
 */
public class VariableScopeExtractAdapter implements TextMap {
    private final Map<String, String> map;
    private static final Set<String> OC_PROPS = ImmutableSet.of("trace-id", "span-id", "parent-span-id", "flags");

    public VariableScopeExtractAdapter(@NonNull final VariableScope execution) {
        Objects.requireNonNull(execution);
        map = execution.getVariables()
                .entrySet()
                .stream()
                .filter(e -> OC_PROPS.contains(e.getKey()))
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), String.valueOf(e.getValue()).replaceAll("\"", "")))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("This class should be used only with Tracer#extract()");
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        return map.entrySet().iterator();
    }
}
