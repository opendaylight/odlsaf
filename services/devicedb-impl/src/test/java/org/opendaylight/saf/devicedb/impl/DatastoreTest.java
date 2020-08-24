/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.saf.devicedb.impl.TestHelper.waitForDeviceOpState;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.jsonrpc.impl.JsonRpcPathBuilder;
import org.opendaylight.jsonrpc.impl.JsonRpcPathCodec;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.saf.governance.client.GovernanceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Main.class, TestConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DatastoreTest {
    private static final JsonElement DEVICES_PATH = JsonRpcPathBuilder.newBuilder("saf-device-database:devices")
            .build();
    private static final JsonElement ICX_PATH = Util.localPath("icx", LogicalDatastoreType.CONFIGURATION);
    private static final JsonElement ICX2_PATH = Util.localPath("icx2", LogicalDatastoreType.CONFIGURATION);
    private static final JsonElement ICX_DATA = TestHelper.readFileAsJson("icx.json");

    @Autowired
    private RemoteOmShard datastore;

    @Autowired
    private DOMDataBroker dataBroker;

    @Autowired
    private JsonRpcPathCodec pathCodec;

    @MockBean
    private GovernanceClient governanceClient;

    @Before
    public void setUp() {
        RemoteOmShard lsc = mock(RemoteOmShard.class);
        when(lsc.commit(anyString())).thenReturn(Boolean.TRUE);
        when(lsc.txid()).thenReturn(UUID.randomUUID().toString());
        when(governanceClient.set(anyString(), anyString())).thenReturn(true);
    }

    /**
     * Wipe config datastore after each test.
     */
    @After
    public void tearDown() {
        final String tx = datastore.txid();
        datastore.delete(tx, 0, Util.SELF_NAME, DEVICES_PATH);
        datastore.commit(tx);
    }

    /**
     * Test same sequence of datastore operations as done by restconf. Sequence is as following (no device exists yet):
     *
     * <p>
     * <ol>
     * <li>READ, path to device, return null</li>
     * <li>TX, allocated txid</li>
     * <li>MERGE, data {}, path to devices container</li>
     * <li>PUT, data is device itself, path to device</li>
     * <li>COMMIT</li>
     * </ol>
     * This will result in APPEARED DTC, however subsequent addition of new device (or modification of existing one)
     * will emit SUBTREE_MODIFIED DTC.
     *
     */
    @Test
    public void testAddDeviceByODL() {
        assertNull(datastore.read(0, Util.SELF_NAME, ICX_PATH));
        String tx = datastore.txid();
        datastore.merge(tx, 0, Util.SELF_NAME, DEVICES_PATH, new JsonObject());
        datastore.put(tx, 0, Util.SELF_NAME, ICX_PATH, ICX_DATA);
        assertTrue(datastore.commit(tx));
        waitForDeviceOpState(dataBroker, pathCodec, "icx", true, 10);
        // wait for entry in operational store to appear
        final JsonObject icxCopy = ICX_DATA.deepCopy().getAsJsonObject();
        icxCopy.addProperty("address", "127.0.0.1");
        // update same device, change eg. address
        assertNotNull(datastore.read(0, Util.SELF_NAME, ICX_PATH));
        tx = datastore.txid();
        datastore.merge(tx, 0, Util.SELF_NAME, DEVICES_PATH, new JsonObject());
        datastore.put(tx, 0, Util.SELF_NAME, ICX_PATH, icxCopy);
        assertTrue(datastore.commit(tx));

        // add another device
        final JsonObject icx2Copy = ICX_DATA.deepCopy().getAsJsonObject();
        icx2Copy.addProperty("entity", "icx2");
        assertNull(datastore.read(0, Util.SELF_NAME, ICX2_PATH));
        tx = datastore.txid();
        datastore.merge(tx, 0, Util.SELF_NAME, DEVICES_PATH, new JsonObject());
        datastore.put(tx, 0, Util.SELF_NAME, ICX2_PATH, icx2Copy);
        assertTrue(datastore.commit(tx));
        waitForDeviceOpState(dataBroker, pathCodec, "icx2", true, 10);

        // delete first icx and verify its op state vanished
        assertTrue(datastore.exists(0, Util.SELF_NAME, ICX_PATH));
        tx = datastore.txid();
        datastore.delete(tx, 0, Util.SELF_NAME, ICX_PATH);
        assertTrue(datastore.commit(tx));
        assertFalse(datastore.exists(0, Util.SELF_NAME, ICX_PATH));
        waitForDeviceOpState(dataBroker, pathCodec, "icx", false, 10);
    }
}
