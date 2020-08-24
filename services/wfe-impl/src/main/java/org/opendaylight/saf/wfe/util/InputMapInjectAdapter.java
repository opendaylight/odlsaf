/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapAdapter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Similar to {@link TextMapAdapter} but with {@link Object} as value-type in carrier {@link Map}.
 *
 * @author <a href="mailto:rkosegi@luminanetoworks.com">Richard Kosegi</a>
 * @since Mar 17, 2020
 */
public class InputMapInjectAdapter implements TextMap {
    private final Map<String, Object> map;

    public InputMapInjectAdapter(@NonNull final Map<String, Object> map) {
        this.map = Objects.requireNonNull(map);
    }

    @Override
    public void put(String key, String value) {
        map.put(key, value);
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("This class should be used only with Tracer#inject()");
    }
}
