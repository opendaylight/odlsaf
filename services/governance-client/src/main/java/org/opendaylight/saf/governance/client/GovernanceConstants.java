/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.client;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * Governance-related constants.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 1, 2020
 */
public final class GovernanceConstants {
    public static final Set<String> STORES = ImmutableSet.<String>builderWithExpectedSize(2)
            .add("config")
            .add("operational")
            .build();

    private GovernanceConstants() {
        // NOOP
    }
}
