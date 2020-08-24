/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.uinv.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class Util {
    private Util() {
        // noop
    }

    /**
     * Get {@link InstanceIdentifier} of node within ODL's inventory for topologies such as Openflow. Usage of this is
     * deprecated but we need to stick with it until OD migrates.
     *
     * @param nodeId node id
     * @return {@link InstanceIdentifier} of node.
     */
    static InstanceIdentifier<Node> nodeInventoryId(String nodeId) {
        return InstanceIdentifier.create(Nodes.class).child(Node.class, new NodeKey(new NodeId(nodeId)));
    }
}
