/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.saf.wfe.util.DelegateUtils.removeArrayElement;
import static org.opendaylight.saf.wfe.util.TestConstants.PARSER;
import static org.opendaylight.saf.wfe.util.TestUtils.readResourceAsText;

import com.google.gson.JsonElement;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * Tests for {@link DelegateUtils}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 28, 2019
 */
public class DelegateUtilsTest {
    private static final String[][] YANG_PATHS = {
            { "brocade-icx-interfaces:interfaces/ethernet/interface/ethernet%201%2F1%2F2",
                "brocade-icx-interfaces:interfaces/ethernet/interface/{ethernet 1/1/2}" },
            { "openconfig-interfaces:interfaces", "openconfig-interfaces:interfaces" } };
    private static final String PATH1 = "config/jsonrpc:config/configured-endpoints/"
            + "arista-eos/yang-ext:mount/openconfig-interfaces:interfaces";
    private static final String PATH2 = "config/jsonrpc:config/configured-endpoints/junos";

    @Test
    public void getEncodedPathTest() {
        for (String[] args : YANG_PATHS) {
            assertEquals(args[0], DelegateUtils.getEncodedPath(args[1]));
        }
    }

    @Test
    public void testExtractDeviceName() {
        assertEquals("arista-eos", DelegateUtils.extractDeviceName(PATH1));
        assertEquals("junos", DelegateUtils.extractDeviceName(PATH2));
    }

    @Test
    public void testRemoveArrayElement() {
        IntStream.range(1, 3).forEach(testcase -> {
            final JsonElement expected = PARSER.parse(readResourceAsText("array-elem" + testcase + "-out.json"));
            final JsonElement in = PARSER.parse(readResourceAsText("array-elem" + testcase + "-in.json"));
            assertEquals(expected, removeArrayElement(in));
        });
    }
}
