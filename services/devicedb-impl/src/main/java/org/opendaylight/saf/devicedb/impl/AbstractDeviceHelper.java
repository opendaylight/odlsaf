/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import com.google.gson.Gson;
import java.util.Optional;
import org.opendaylight.jsonrpc.impl.JsonConverter;
import org.opendaylight.jsonrpc.impl.JsonRpcPathCodec;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Common code to deal with devices.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 5, 2020
 */
public abstract class AbstractDeviceHelper {
    @Autowired
    protected JsonConverter jsonConverter;

    @Autowired
    protected DOMDataBroker domDataBroker;

    @Autowired
    protected JsonRpcPathCodec pathCodec;

    @Autowired
    protected Gson gson;

    /**
     * Attempt to read device from datastore and convert it to given type.
     *
     * @param <T> target type to convert to
     * @param device name of device
     * @param datastore type of datastore
     * @param klass runtime type to convert to
     * @return {@link Optional} of device
     */
    protected <T> Optional<T> readDevice(String device, LogicalDatastoreType datastore, Class<T> klass) {
        final YangInstanceIdentifier yii = Util.yiiForDevice(pathCodec, device, datastore);
        try (DOMDataTreeReadTransaction rtx = domDataBroker.newReadOnlyTransaction()) {
            return Util.getUnchecked(rtx.read(datastore, Util.yiiForDevice(pathCodec, device, datastore))).map(node -> {
                return gson.fromJson(jsonConverter.toBus(yii, node).getData(), klass);
            });
        }
    }
}
