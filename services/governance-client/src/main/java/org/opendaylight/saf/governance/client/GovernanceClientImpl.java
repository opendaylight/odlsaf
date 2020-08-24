/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.client;

import com.google.gson.JsonElement;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SetGovernanceInput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.UnsetGovernanceInput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.PublishInput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.modules.Modules;

/**
 * Implementation of {@link GovernanceClient}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 1, 2020
 */
public class GovernanceClientImpl implements GovernanceClient, AutoCloseable {
    private GovernanceCompositeInterface proxy;

    public GovernanceClientImpl(TransportFactory transportFactory, String endpoint) throws URISyntaxException {
        proxy = transportFactory.endpointBuilder()
                .requester()
                .createProxy(GovernanceCompositeInterface.class, endpoint);
    }

    @Override
    public Optional<String> governance(String entity, String store, JsonElement path) {
        return Optional.ofNullable(proxy.governance(store, entity, path).getUri());
    }

    @Override
    public boolean set(String entity, String store, JsonElement path, String uri) {
        return proxy.setGovernance(SetGovernanceInput.builder().entity(entity).path(path).store(store).uri(uri).build())
                .getSuccess();
    }

    @Override
    public void unset(String entity, String store, JsonElement path) {
        proxy.unsetGovernance(UnsetGovernanceInput.builder().entity(entity).store(store).path(path).build());
    }

    @Override
    public void close() {
        proxy.close();
    }

    @Override
    public boolean publish(Collection<Modules> modules) {
        return proxy.publish(PublishInput.builder().modules(new ArrayList<>(modules)).build()).getSuccess();
    }
}
