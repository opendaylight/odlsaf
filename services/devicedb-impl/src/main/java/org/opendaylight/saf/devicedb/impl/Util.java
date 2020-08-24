/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import static org.opendaylight.jsonrpc.bus.messagelib.Util.injectQueryParam;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.impl.JsonRpcPathBuilder;
import org.opendaylight.jsonrpc.impl.JsonRpcPathCodec;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Collection of utility methods.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Feb 26, 2020
 */
public final class Util {
    static final String SELF_NAME = "devicedb";
    private static final JsonPrimitive DEFAULT_TIMEOUT = new JsonPrimitive(90_000);

    private Util() {
        // utility class
    }

    /**
     * Get JsonRpc path encoded as {@link JsonObject} for given device that is local to this service (specific for
     * Device DB).
     *
     * @param device name of device
     * @param datastore datastore type
     * @return encoded path
     */
    public static JsonObject localPath(String device, LogicalDatastoreType datastore) {
        if (LogicalDatastoreType.CONFIGURATION.equals(datastore)) {
            return JsonRpcPathBuilder.newBuilder("saf-device-database:devices")
                    .container("device")
                    .item("entity", device)
                    .build();
        } else {
            return JsonRpcPathBuilder.newBuilder("saf-device-database:devices-state")
                    .container("device-state")
                    .item("entity", device)
                    .build();
        }
    }

    /**
     * Get JsonRpc path encoded as {@link JsonObject} for given device specific for LSC.
     *
     * @param device name of device
     * @return encoded path
     */
    public static JsonObject lscPath(String device) {
        return JsonRpcPathBuilder.newBuilder("jsonrpc:config")
                .container("configured-endpoints")
                .item("name", device)
                .build();
    }

    /**
     * Get result of given future, rethrowing {@link ExecutionException} as {@link IllegalStateException}.
     *
     * @param <V> type of return value
     * @param future {@link Future} object used to compute value
     * @return computed value
     */
    public static <V> V getUnchecked(Future<V> future) {
        try {
            return Uninterruptibles.getUninterruptibly(future);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to compute result", e);
        }
    }

    /**
     * Create {@link YangInstanceIdentifier} for device.
     *
     * @param pathCodec {@link JsonRpcPathCodec} used to deserialize json path
     * @param name name of device
     * @param datastore datastore type
     * @return deserialized path
     */
    public static YangInstanceIdentifier yiiForDevice(JsonRpcPathCodec pathCodec, String name,
            LogicalDatastoreType datastore) {
        return pathCodec.deserialize(localPath(name, datastore));
    }

    /**
     * Consolidate nullable boolean value as primitive boolean.
     *
     * @param nullableBool {@link Boolean} to consolidate
     * @return true if and only if nullableBool is not null and is equal to {@link Boolean#TRUE}
     */
    public static boolean isTrue(@Nullable Boolean nullableBool) {
        return Optional.ofNullable(nullableBool).orElse(Boolean.FALSE);
    }

    /**
     * Compose URI that is supposed to handle given device.
     *
     * @param data {@link JsonObject} containing device configuration
     * @param handlerService upstream service that will handle data operation requests
     * @return composed URI
     */
    public static String getHandlerUri(JsonObject data, String handlerService) {
        final int timeout = Optional.ofNullable(data.get("timeout")).orElse(DEFAULT_TIMEOUT).getAsInt();
        String uri = injectQueryParam(handlerService, "timeout", String.valueOf(timeout));
        uri = injectQueryParam(uri, "channel", data.get("entity").getAsString());
        return uri;
    }
}
