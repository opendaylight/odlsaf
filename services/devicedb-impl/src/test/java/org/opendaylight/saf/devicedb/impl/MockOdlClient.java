/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MockOdlClient implements OdlClient {

    @Override
    public boolean mount(String name, Set<String> modules) {
        return true;
    }

    @Override
    public boolean unmount(String name) {
        return true;
    }
}
