/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.devicesState.deviceState.DeviceState;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * COmponent that reflects device state into operational datastore.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 3, 2020
 */
@Component
public class OpstatePublisher extends AbstractDeviceHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OpstatePublisher.class);

    /**
     * Update operational state for given device.
     *
     * @param device name of device
     * @param mountStatus mount status
     */
    public void publish(String device, String mountStatus) {
        LOG.info("Updating mount state for device '{}' to '{}'", device, mountStatus);
        final DeviceState state = new DeviceState();
        state.setLockState(false);
        state.setEntity(device);
        state.setMountStatus(mountStatus);
        // if opstate already exists, copy lock flag so it won't get overwritten
        readDevice(device, LogicalDatastoreType.OPERATIONAL, DeviceState.class)
                .ifPresent(existing -> state.setLockState(existing.getLockState()));
        final YangInstanceIdentifier yii = Util.yiiForDevice(pathCodec, device, LogicalDatastoreType.OPERATIONAL);
        final DOMDataTreeWriteTransaction wtx = domDataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, yii,
                jsonConverter.jsonElementToNormalizedNode(gson.toJsonTree(state), yii, true));
        Util.getUnchecked(wtx.commit());
    }

    /**
     * Unpublish (delete) operational state from datastore.
     *
     * @param device device to unpublish.
     */
    public void unpublish(String device) {
        LOG.info("Removing operational state for device '{}'", device);
        final DOMDataTreeWriteTransaction wtx = domDataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.OPERATIONAL,
                Util.yiiForDevice(pathCodec, device, LogicalDatastoreType.OPERATIONAL));
        Util.getUnchecked(wtx.commit());
    }
}
