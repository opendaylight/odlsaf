/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory;

/**
 * Simple wrapper around 2 in-memory datastores to allow simple cleanup.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Nov 26, 2019
 */
public class DatastoreContext implements AutoCloseable {
    private final InMemoryDOMDataStore opDatastore;
    private final InMemoryDOMDataStore configDatastore;
    private final Map<LogicalDatastoreType, InMemoryDOMDataStore> storeMap;

    public DatastoreContext(DOMSchemaService schemaService) {
        opDatastore = InMemoryDOMDataStoreFactory.create(LogicalDatastoreType.OPERATIONAL.name(), schemaService);
        configDatastore = InMemoryDOMDataStoreFactory.create(LogicalDatastoreType.CONFIGURATION.name(), schemaService);
        storeMap = ImmutableMap.<LogicalDatastoreType, InMemoryDOMDataStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, opDatastore)
                .put(LogicalDatastoreType.CONFIGURATION, configDatastore)
                .build();

    }

    @Override
    public void close() throws Exception {
        storeMap.values().forEach(InMemoryDOMDataStore::close);
    }

    public DOMStore getStore(LogicalDatastoreType type) {
        Objects.requireNonNull(type, "Datastore type can't be NULL");
        return Optional.ofNullable(storeMap.get(type))
                .orElseThrow(() -> new IllegalArgumentException("No such datastore type : " + type));
    }

    public Map<LogicalDatastoreType, DOMStore> getStoreMap() {
        return ImmutableMap.copyOf(storeMap);
    }
}
