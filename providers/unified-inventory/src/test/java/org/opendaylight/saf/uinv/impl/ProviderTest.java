/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.uinv.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.devicedb.rev160608.Devices;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.devicedb.rev160608.DevicesBuilder;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.devicedb.rev160608.devices.DeviceBuilder;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.GetTopologyInputBuilder;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.GetTopologyOutput;
import org.opendaylight.yang.gen.v1.https.opendaylight.org.saf.uinified.inventory.rev190404.TopologySource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderTest extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(ProviderTest.class);
    private UnifiedTopologyServiceImpl provider;
    private MountPointService mountPointService;

    @Override
    protected void setupWithSchema(EffectiveModelContext context) {
        super.setupWithSchema(context);
        mountPointService = mock(MountPointService.class);
        provider = new UnifiedTopologyServiceImpl(getDataBroker(), mountPointService);
    }

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return Sets.newHashSet(BindingReflections.getModuleInfo(Config.class),
                BindingReflections.getModuleInfo(NetworkTopology.class),
                BindingReflections.getModuleInfo(Devices.class));
    }

    @Test
    public void testGetODLDevices() throws InterruptedException, ExecutionException {
        final WriteTransaction wtx = getDataBroker().newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(NetworkTopology.class).build(),
                new NetworkTopologyBuilder()
                        .setTopology(
                                Maps.uniqueIndex(Lists.newArrayList(
                                        new TopologyBuilder().setTopologyId(new TopologyId("topology-1"))
                                                .setNode(Maps.uniqueIndex(Lists.newArrayList(
                                                        new NodeBuilder().setNodeId(new NodeId("node-1")).build()),
                                                        Identifiable::key))
                                                .build(),
                                        new TopologyBuilder().setTopologyId(new TopologyId("flow:1"))
                                                .setNode(Maps.uniqueIndex(Lists.newArrayList(
                                                        new NodeBuilder().setNodeId(new NodeId("of-node-1")).build()),
                                                        Identifiable::key))
                                                .build()),
                                        Identifiable::key))
                        .build());
        wtx.commit().get();

        final ListenableFuture<RpcResult<GetTopologyOutput>> output = provider
                .getTopology(new GetTopologyInputBuilder().setTopologySource(TopologySource.ODL).build());
        final GetTopologyOutput result = output.get().getResult();
        LOG.info("Output : {}", result);
        assertEquals(1,
                Iterables.get(result.getUnifiedNetworkTopology().getUnifiedTopology().values(), 0).getNodes().size());
    }

    @Test
    public void testGetSAFDevicesNotPresent() throws InterruptedException, ExecutionException {
        final ListenableFuture<RpcResult<GetTopologyOutput>> output = provider
                .getTopology(new GetTopologyInputBuilder().setTopologySource(TopologySource.SAF).build());
        LOG.info("Output : {}", output.get().getResult());
        assertEquals(0, output.get().getResult().getUnifiedNetworkTopology().nonnullUnifiedTopology().size());
    }

    @Test
    public void testGetSAFDevices() throws InterruptedException, ExecutionException {
        final WriteTransaction wtx = getDataBroker().newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Devices.class).build(),
                new DevicesBuilder()
                        .setDevice(
                                Maps.uniqueIndex(Lists.newArrayList(new DeviceBuilder().setEntity("device-1").build()),
                                        Identifiable::key))
                        .build());
        wtx.commit().get();

        final MountPoint mp = mock(MountPoint.class);
        when(mp.getService(eq(DataBroker.class))).thenReturn(Optional.of(getDataBroker()));
        when(mountPointService.getMountPoint(any())).thenReturn(Optional.of(mp));
        final ListenableFuture<RpcResult<GetTopologyOutput>> output = provider
                .getTopology(new GetTopologyInputBuilder().setTopologySource(TopologySource.SAF).build());
        LOG.info("Output : {}", output.get().getResult());
        assertEquals(1, output.get().getResult().getUnifiedNetworkTopology().nonnullUnifiedTopology().size());
    }
}
