/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import org.opendaylight.jsonrpc.impl.JsonRpcPathCodec;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of helper methods for easier testing.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 8, 2020
 */
public final class TestHelper {
    private static final Logger LOG = LoggerFactory.getLogger(TestHelper.class);
    public static final JsonParser PARSER = new JsonParser();

    private TestHelper() {
        // NOOP
    }

    /**
     * Read file from classpath as {@link JsonObject}.
     *
     * @param name name of file to read
     * @return {@link JsonObject}
     */
    public static JsonObject readFileAsJson(String name) {
        try (InputStream is = Resources.getResource(name).openStream()) {
            return PARSER.parse(new InputStreamReader(is)).getAsJsonObject();
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean waitForDeviceOpState(DOMDataBroker dataBroker, JsonRpcPathCodec pathCodec, String device,
            boolean present, long seconds) {
        final long future = System.currentTimeMillis() + seconds * 1000L;
        for (;;) {
            try (DOMDataTreeReadTransaction rtx = dataBroker.newReadOnlyTransaction()) {
                boolean current = Util.getUnchecked(rtx.read(LogicalDatastoreType.OPERATIONAL,
                        Util.yiiForDevice(pathCodec, device, LogicalDatastoreType.OPERATIONAL))).isPresent();
                if (current == present) {
                    return true;
                } else {
                    LOG.info("Device '{}' operational state presence is {}, but want {}, waiting", current, present,
                            device);
                    Uninterruptibles.sleepUninterruptibly(500L, TimeUnit.MILLISECONDS);
                    if (System.currentTimeMillis() > future) {
                        return false;
                    }
                }
            }
        }
    }

}
