/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.uinv.impl;

import static org.opendaylight.yangtools.yang.common.RpcResultBuilder.success;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.devicedb.rev160608.Devices;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.devicedb.rev160608.devices.Device;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.devicedb.rev160608.devices.DeviceKey;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.GetTopologyInput;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.GetTopologyOutput;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.GetTopologyOutputBuilder;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.SafUnifiedInventoryService;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.unified.topology.UnifiedNetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.unified.topology.unified.network.topology.UnifiedTopology;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.unified.topology.unified.network.topology.UnifiedTopologyBuilder;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.unified.topology.unified.network.topology.unified.topology.Nodes;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.unified.topology.unified.network.topology.unified.topology.NodesBuilder;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.unified.topology.unified.network.topology.unified.topology.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class UnifiedTopologyServiceImpl implements SafUnifiedInventoryService {
    private static final String DEVICEDB_ID = "devicedb";
    private static final String OPENFLOW_ID = "flow:1";
    private static final String OUTPUT_LEAP_TOPOLOGY_ID = "saf-topology";
    private static final InstanceIdentifier<Devices> DEVICES_II = InstanceIdentifier.create(Devices.class);
    private static final InstanceIdentifier<NetworkTopology> NT_II = InstanceIdentifier.create(NetworkTopology.class);
    private static final InstanceIdentifier<ConfiguredEndpoints> DEVICEDB_MP_II = InstanceIdentifier
            .create(Config.class)
            .child(ConfiguredEndpoints.class, new ConfiguredEndpointsKey(DEVICEDB_ID));

    private final DataBroker dataBroker;
    private final MountPointService mountPointService;

    public UnifiedTopologyServiceImpl(@NonNull final DataBroker dataBroker,
            @NonNull final MountPointService mountPointService) {
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.mountPointService = Objects.requireNonNull(mountPointService);
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyOutput>> getTopology(GetTopologyInput input) {
        final List<UnifiedTopology> topoList = new ArrayList<>();

        switch (input.getTopologySource()) {
            case SAF:
                readDeviceDb().ifPresent(topoList::add);
                break;

            case ODL:
                topoList.addAll(readOdlDevices());
                break;

            case ALL:
                readDeviceDb().ifPresent(topoList::add);
                topoList.addAll(readOdlDevices());
                break;

            default:
                return RpcResultBuilder.<GetTopologyOutput>failed()
                        .withError(ErrorType.APPLICATION,
                                "Topology source not supported : " + input.getTopologySource())
                        .buildFuture();
        }

        return success(new GetTopologyOutputBuilder().setUnifiedNetworkTopology(
                new UnifiedNetworkTopologyBuilder().setUnifiedTopology(Maps.uniqueIndex(topoList, Identifiable::key))
                        .build())
                .build()).buildFuture();
    }

    private List<UnifiedTopology> readOdlDevices() {
        try (ReadTransaction rtx = dataBroker.newReadOnlyTransaction()) {
            return Futures.getUnchecked(rtx.read(LogicalDatastoreType.OPERATIONAL, NT_II))
                    .stream()
                    .map(NetworkTopology::getTopology)
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .map(UnifiedTopologyServiceImpl::mapTopology)
                    .collect(Collectors.toList());
        }
    }

    private static UnifiedTopology mapTopology(Topology topology) {
        return new UnifiedTopologyBuilder().setTopologyId(topology.getTopologyId().getValue())
                .setNodes(Optional.ofNullable(topology.getNode())
                        .orElse(Collections.emptyMap())
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(entry -> new NodesKey(entry.getKey().getNodeId().getValue()),
                            entry -> mapOdlNode(entry.getValue(), topology.getTopologyId()))))
                .build();
    }

    private static Nodes mapOdlNode(Node node, TopologyId topoId) {
        final NodeId nodeId = node.getNodeId();
        final NodesBuilder builder = new NodesBuilder().setNodeId(nodeId.getValue());
        if (topoId.getValue().equals(OPENFLOW_ID)) {
            builder.setNodeReference(Util.nodeInventoryId(nodeId.getValue()));
        } else {
            builder.setNodeReference(InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(topoId))
                    .child(Node.class, new NodeKey(nodeId)));
        }
        return builder.build();
    }

    /**
     * Fetch SAF devices from DeviceDB.
     *
     * @return {@link Optional} of {@link UnifiedTopology} with value present if DeviceDB mount point exists and
     *         devices are readable (and present).
     */
    private Optional<UnifiedTopology> readDeviceDb() {
        final Optional<MountPoint> mp = mountPointService.getMountPoint(DEVICEDB_MP_II);
        if (mp.isEmpty()) {
            return Optional.empty();
        }
        final Optional<DataBroker> dbOpt = mp.get().getService(DataBroker.class);
        if (dbOpt.isPresent()) {
            try (ReadTransaction rtx = dbOpt.get().newReadOnlyTransaction()) {
                return Futures.getUnchecked(rtx.read(LogicalDatastoreType.OPERATIONAL, DEVICES_II))
                        .stream()
                        .map(device -> new UnifiedTopologyBuilder().setTopologyId(OUTPUT_LEAP_TOPOLOGY_ID)
                                .setNodes(Maps.uniqueIndex(device.nonnullDevice()
                                        .values()
                                        .stream()
                                        .map(UnifiedTopologyServiceImpl::mapSafNode)
                                        .collect(Collectors.toList()), Identifiable::key))
                                .build())
                        .findFirst();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Map SAF {@link Device} into {@link Nodes}.
     *
     * @param node instance of {@link Device} to map
     * @return {@link Nodes}
     */
    private static Nodes mapSafNode(Device node) {
        return new NodesBuilder().setNodeId(node.getEntity())
                .setNodeReference(
                        InstanceIdentifier.create(Devices.class).child(Device.class, new DeviceKey(node.getEntity())))
                .setAddress(node.getAddress())
                .setModules(node.getModules())
                .setDeviceType(node.getDeviceType())
                .build();
    }
}
