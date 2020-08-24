/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Optional;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.springframework.stereotype.Service;

@Service
public class Governance implements AutoCloseable {
    private final LoadingCache<String, HierarchicalEnumMap<JsonElement, DataType, String>> cache = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<String, HierarchicalEnumMap<JsonElement, DataType, String>>() {
                @Override
                public HierarchicalEnumMap<JsonElement, DataType, String> load(String key) throws Exception {
                    return HierarchicalEnumHashMap.create(DataType.class, JsonPathCodec.create());
                }
            });

    @Nullable
    public String get(@Nullable String entity, @NonNull JsonElement path, @NonNull String store) {
        return cache.getUnchecked(ensureEntity(entity)).lookup(path, fromStore(store)).orElse(null);
    }

    public void set(@Nullable String entity, @NonNull JsonElement path, @NonNull String store, @Nullable String data) {
        cache.getUnchecked(ensureEntity(entity)).put(path, fromStore(store), data);
    }

    public void delete(@Nullable String entity, @NonNull JsonElement path, @NonNull String store) {
        set(entity, path, store, null);
    }

    /*
     * Entity can be NULL, but that is not friendly with CacheLoader, so use empty string instead
     */
    private static String ensureEntity(@Nullable String entity) {
        return Optional.ofNullable(entity).orElse("");
    }

    /*
     * Only "operational" and "config" are really used for store, so RPC can be used as placeholder for everything else
     */
    private static DataType fromStore(String store) {
        try {
            return DataType.forDatastore(Util.storeFromString(store));
        } catch (IllegalArgumentException | NullPointerException e) {
            // ignore any error at this point
            return DataType.RPC;
        }
    }

    @Override
    public void close() throws Exception {
        cache.asMap().clear();
    }
}
