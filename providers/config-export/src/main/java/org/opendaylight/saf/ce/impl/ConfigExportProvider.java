/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.ce.impl;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.DataExportImportService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.RelativeTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.ScheduleExportInput.RunAt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.ScheduleExportInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.ScheduleExportOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.WildcardStar;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.exclusions.ExcludedModules.ModuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.daexim.rev160921.exclusions.ExcludedModulesBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigExportProvider implements DOMMountPointListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigExportProvider.class);
    private final ListenerRegistration<DOMMountPointListener> registration;
    private final DataExportImportService daeximProxy;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1,
            new ThreadFactoryBuilder().setDaemon(false).setNameFormat("config-export-%d").build());

    public ConfigExportProvider(DOMMountPointService mountPointService, RpcConsumerRegistry consumerRegistry) {
        registration = mountPointService.registerProvisionListener(this);
        daeximProxy = consumerRegistry.getRpcService(DataExportImportService.class);
        LOG.info("Configuration export provider started");
    }

    @Override
    public void close() throws Exception {
        registration.close();
        threadPool.shutdown();
        LOG.info("Configuration export provider closed");
    }

    @Override
    public void onMountPointCreated(YangInstanceIdentifier path) {
        LOG.debug("Mountpoint created at {}", path);
        processEvent();
    }

    @Override
    public void onMountPointRemoved(YangInstanceIdentifier path) {
        LOG.debug("Mountpoint removed at {}", path);
        processEvent();
    }

    private void processEvent() {
        // dispatch in separate single-threaded pool that will do queuing
        threadPool.execute(() -> {
            final ListenableFuture<RpcResult<ScheduleExportOutput>> future = daeximProxy
                    .scheduleExport(new ScheduleExportInputBuilder().setLocalNodeOnly(true)
                            .setExcludedModules(Maps.uniqueIndex(Collections.singletonList(
                                    new ExcludedModulesBuilder().setDataStore(new DataStore("operational"))
                                            .setModuleName(new ModuleName(new WildcardStar("*")))
                                            .build()),
                                    Identifiable::key))
                            .setRunAt(new RunAt(new RelativeTime(Uint32.TEN)))
                            .build());
            Futures.addCallback(future, new FutureCallback<RpcResult<ScheduleExportOutput>>() {
                @Override
                public void onSuccess(RpcResult<ScheduleExportOutput> result) {
                    LOG.debug("Export scheduled with result {}", result.getResult());
                }

                @Override
                public void onFailure(Throwable cause) {
                    LOG.warn("Export failed", cause);
                }
            }, MoreExecutors.directExecutor());
        });
    }
}
