/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.util;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

/**
 * Test constants.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jun 11, 2019
 */
public final class TestConstants {
    public static final String GET_REQ_167 = "{\"workflow-name\":\"GET\",\"workflow-input\":{\"array-element\":"
            + "[{\"yang-path\":\"brocade-mlx-interfaces:interfaces/ethernet/interface/{invalid}\"}]}}";
    public static final JsonParser PARSER = new JsonParser();
    public static final Gson GSON = new Gson();

    private TestConstants() {
        // utility class constructor
    }
}
