/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URISyntaxException;
import java.util.Set;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "odl")
public class OdlClientImpl implements OdlClient {
    @Value("${odl}")
    private String odlEndpoint;

    @Autowired
    private TransportFactory transportFactory;

    @Autowired
    private Gson gson;

    @Override
    public boolean mount(String name, Set<String> modules) {
        try (RemoteOmShard store = transportFactory.endpointBuilder()
                .requester()
                .createProxy(RemoteOmShard.class, odlEndpoint)) {
            final JsonObject root = new JsonObject();
            root.addProperty("name", name);
            root.add("modules", gson.toJsonTree(modules));
            final String tx = store.txid();
            store.put(tx, 0, "", Util.lscPath(name), root);
            return store.commit(tx);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid LSC endpoint", e);
        }
    }

    @Override
    public boolean unmount(String name) {
        try (RemoteOmShard store = transportFactory.endpointBuilder()
                .requester()
                .createProxy(RemoteOmShard.class, odlEndpoint)) {
            final String tx = store.txid();
            store.delete(tx, 0, "", Util.lscPath(name));
            return store.commit(tx);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid LSC endpoint", e);
        }
    }
}
