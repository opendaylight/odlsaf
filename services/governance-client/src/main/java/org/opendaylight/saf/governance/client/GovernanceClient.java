/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Optional;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SafGovernanceRpcService;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.SafYangLibraryRpcService;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.modules.Modules;

/**
 * Governance client.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 1, 2020
 */
public interface GovernanceClient {
    /**
     * Set governance for given entity (both config and operational datastore). This operation might
     * <strong>NOT</strong> be atomic in all cases.
     *
     * @param entity entity to set
     * @param uri endpoint URI of datastore implementation
     * @return true if operation was successful, false otherwise
     *
     * @see SafGovernanceRpcService#setGovernance(String, String, String, com.google.gson.JsonElement)
     */
    default boolean set(String entity, String uri) {
        return GovernanceConstants.STORES.stream().map(store -> set(entity, store, uri)).allMatch(Boolean.TRUE::equals);
    }

    /**
     * Set governance for given entity.
     *
     * @param entity entity to set governance for
     * @param store datastore type
     * @param uri endpoint URI of datastore implementation
     * @return true if operation was successful, false otherwise
     *
     * @see SafGovernanceRpcService#setGovernance(String, String, String, com.google.gson.JsonElement)
     */
    default boolean set(String entity, String store, String uri) {
        return set(entity, store, new JsonObject(), uri);
    }

    /**
     * Set governance for given entity.
     *
     * @param entity entity to set governance for
     * @param store datastore type
     * @param uri endpoint URI of datastore implementation
     * @param path path specifying the model subtree
     * @return true if operation was successful, false otherwise
     *
     * @see SafGovernanceRpcService#setGovernance(String, String, String, com.google.gson.JsonElement)
     */
    boolean set(String entity, String store, JsonElement path, String uri);

    /**
     * Unset governance for given device (both config and operational datastore).
     *
     * @param entity entity to unset
     */
    default void unset(String entity) {
        GovernanceConstants.STORES.forEach(store -> unset(entity, store));
    }

    /**
     * Unset governance for given entity.
     *
     * @param entity entity to unset
     * @param store datastore type
     *
     * @see SafGovernanceRpcService#unsetGovernance(String, String, com.google.gson.JsonElement)
     */
    default void unset(String entity, String store) {
        unset(entity, store, new JsonObject());
    }

    /**
     * Unset governance for given entity.
     *
     * @param entity entity to unset
     * @param store datastore type
     * @param path path specifying the model subtree
     *
     * @see SafGovernanceRpcService#unsetGovernance(String, String, com.google.gson.JsonElement)
     */
    void unset(String entity, String store, JsonElement path);

    /**
     * Get URI of endpoint governing given entity.
     *
     * @param entity name of entiry to find endpoint for
     * @param store datastore
     * @param path YANG path
     * @return {@link Optional} of URI
     */
    Optional<String> governance(String entity, String store, JsonElement path);

    /**
     * Publish set of YANG modules into library.
     *
     * @param modules modules to publish
     * @return flag to indicate result of operation
     * @see SafYangLibraryRpcService#publish(java.util.List)
     */
    boolean publish(Collection<Modules> modules);
}
