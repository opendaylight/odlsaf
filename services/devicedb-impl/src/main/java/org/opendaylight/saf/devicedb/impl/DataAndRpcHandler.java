/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import static org.opendaylight.saf.devicedb.impl.Util.getUnchecked;
import static org.opendaylight.saf.devicedb.impl.Util.yiiForDevice;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.impl.JsonConverter;
import org.opendaylight.jsonrpc.impl.JsonRpcDatastoreAdapter;
import org.opendaylight.jsonrpc.model.AddListenerArgument;
import org.opendaylight.jsonrpc.model.DataOperationArgument;
import org.opendaylight.jsonrpc.model.DeleteListenerArgument;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;
import org.opendaylight.jsonrpc.model.TxArgument;
import org.opendaylight.jsonrpc.model.TxOperationArgument;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.DeviceLockInput;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.DeviceLockOutput;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.SafDeviceDatabaseRpcService;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.devicesState.deviceState.DeviceState;
import org.opendaylight.saf.springboot.annotation.Responder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Responder("${operations}")
public class DataAndRpcHandler extends AbstractDeviceHelper implements RemoteOmShard, SafDeviceDatabaseRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(DataAndRpcHandler.class);
    private final JsonRpcDatastoreAdapter datastore;

    public DataAndRpcHandler(@Autowired SchemaContext schemaContext, @Autowired DOMDataBroker domDataBroker,
            @Autowired JsonConverter jsonConverter, @Autowired TransportFactory transportFactory) {
        datastore = new JsonRpcDatastoreAdapter(jsonConverter, domDataBroker, schemaContext, transportFactory, false);
    }

    @Override
    public DeviceLockOutput deviceLock(DeviceLockInput input) {
        // TODO : inbound argument checking should be done in automated manner (maybe flag in @Responder annotation)
        Preconditions.checkArgument(input != null, "Input object is missing");
        Preconditions.checkArgument(input.getEntity() != null, "Device name is missing");
        Preconditions.checkArgument(input.getLockState() != null, "Lock state is missing");
        final String deviceName = input.getEntity();
        final YangInstanceIdentifier path = yiiForDevice(pathCodec, deviceName, LogicalDatastoreType.OPERATIONAL);
        final DeviceState device = readDevice(deviceName, LogicalDatastoreType.OPERATIONAL, DeviceState.class)
                .orElseThrow(
                    () -> new IllegalStateException("Unable to find operational state for device " + deviceName));
        final boolean previousLockState = Optional.ofNullable(device.getLockState()).orElse(Boolean.FALSE);
        final Optional<String> previousLockId = Optional.ofNullable(device.getLockId());
        final Optional<String> desiredLockId = Optional.ofNullable(input.getLockId());
        final boolean desiredLockState = input.getLockState();
        LOG.info("Previous lock state : {}, desired lock state : {}", previousLockState, input.getLockState());
        LOG.info("Previous lock ID : {}, desired lock ID : {}", previousLockId, desiredLockId);
        if (previousLockState == desiredLockState) {
            if (previousLockState) {
                // trying to lock device that was locked already with same ID is allowed
                if (desiredLockId.isPresent() && previousLockId.isPresent()
                        && desiredLockId.get().equals(previousLockId.get())) {
                    return DeviceLockOutput.builder().success(true).build();
                }
                // trying to lock already locked, this is wrong
                return DeviceLockOutput.builder().success(false).build();
            } else {
                // trying to unlock device that has no lock, this is OK but NOOP
                return DeviceLockOutput.builder().success(true).build();
            }
        }
        updateDeviceState(path, desiredLockState, device, desiredLockId);
        return DeviceLockOutput.builder().success(true).build();
    }

    private void updateDeviceState(YangInstanceIdentifier path, boolean desiredLockState, DeviceState device,
            Optional<String> lockId) {
        final DOMDataTreeReadWriteTransaction wtx = domDataBroker.newReadWriteTransaction();
        device.setLockState(desiredLockState);
        lockId.ifPresent(device::setLockId);
        LOG.info("Updating operational state of device {}", device);
        wtx.put(LogicalDatastoreType.OPERATIONAL, path,
                jsonConverter.jsonElementToNormalizedNode(gson.toJsonTree(device), path, true));
        getUnchecked(wtx.commit());
    }

    @Override
    public JsonElement read(StoreOperationArgument arg) {
        return datastore.read(arg);
    }

    @Override
    public void put(DataOperationArgument arg) {
        datastore.put(arg);
    }

    @Override
    public boolean exists(StoreOperationArgument arg) {
        return datastore.exists(arg);
    }

    @Override
    public void merge(DataOperationArgument arg) {
        datastore.merge(arg);
    }

    @Override
    public void delete(TxOperationArgument arg) {
        datastore.delete(arg);
    }

    @Override
    public boolean commit(TxArgument arg) {
        return datastore.commit(arg);
    }

    @Override
    public boolean cancel(TxArgument arg) {
        return datastore.cancel(arg);
    }

    @Override
    public String txid() {
        return datastore.txid();
    }

    @Override
    public List<String> error(TxArgument arg) {
        return datastore.error(arg);
    }

    @Override
    public ListenerKey addListener(AddListenerArgument arg) throws IOException {
        return datastore.addListener(arg);
    }

    @Override
    public boolean deleteListener(DeleteListenerArgument arg) {
        return datastore.deleteListener(arg);
    }

    @Override
    public void close() {
        datastore.close();
    }
}
