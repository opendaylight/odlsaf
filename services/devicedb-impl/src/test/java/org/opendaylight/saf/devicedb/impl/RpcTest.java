/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.saf.devicedb.impl.TestHelper.waitForDeviceOpState;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.UUID;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.jsonrpc.impl.JsonRpcPathBuilder;
import org.opendaylight.jsonrpc.impl.JsonRpcPathCodec;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.SafDeviceDatabaseRpcService;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.devices.device.Device;
import org.opendaylight.saf.governance.client.GovernanceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * RPCs related tests.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 5, 2020
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Main.class, TestConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RpcTest {
    private static final String ENTITY = "device-1";

    @MockBean
    private GovernanceClient controlClient;

    @Autowired
    private SafDeviceDatabaseRpcService rpc;

    @Autowired
    private JsonRpcPathCodec pathCodec;

    @Autowired
    private DOMDataBroker dataBroker;

    @Autowired
    private RemoteOmShard datastore;

    @Autowired
    private Gson gson;

    @After
    public void tearDown() {
        String tx = datastore.txid();
        datastore.delete(tx, "config", "devicedb",
                JsonRpcPathBuilder.newBuilder().container("saf-device-database:devices").build());
        assertTrue(datastore.commit(tx));
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidDevice() {
        assertTrue(waitForDeviceOpState(dataBroker, pathCodec, ENTITY, false, 30));
        rpc.deviceLock(ENTITY, true, null);
    }

    @Test
    public void testLockAndUnlock() {
        // Create device entry

        String tx = datastore.txid();
        JsonElement device = gson.toJsonTree(Device.builder()
                .entity(ENTITY)
                .username("admin")
                .modules(Lists.newArrayList("interfaces"))
                .loginPassword("admin")
                .deviceType("ios")
                .address("127.0.0.1")
                .build());
        datastore.put(tx, "config", "devicedb", Util.localPath(ENTITY, LogicalDatastoreType.CONFIGURATION), device);
        datastore.commit(tx);

        assertTrue(waitForDeviceOpState(dataBroker, pathCodec, ENTITY, true, 30));

        // lock should succeed
        assertTrue(rpc.deviceLock(ENTITY, true, null).getSuccess());
        // second attempt must fail
        assertFalse(rpc.deviceLock(ENTITY, true, null).getSuccess());
        // unlock
        assertTrue(rpc.deviceLock(ENTITY, false, null).getSuccess());
        // subsequent unlock should succeed, even if unlocked already
        assertTrue(rpc.deviceLock(ENTITY, false, null).getSuccess());

        // subsequent locks with same ID should succeed
        final String lockId = UUID.randomUUID().toString();
        assertTrue(rpc.deviceLock(ENTITY, true, lockId).getSuccess());
        assertTrue(rpc.deviceLock(ENTITY, true, lockId).getSuccess());
    }
}
