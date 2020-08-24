/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.DeviceLockInput;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.SafDeviceDatabaseRpcService;
import org.opendaylight.saf.springboot.annotation.RequesterProxy;
import org.opendaylight.saf.wfe.impl.model.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link LockService}.
 *
 * @author spichandi
 *
 */
@Service("lockService")
@ConditionalOnProperty(name = "devicedb")
public class LockServiceImpl implements LockService {
    private static final Logger LOG = LoggerFactory.getLogger(LockServiceImpl.class);

    @RequesterProxy("${devicedb}")
    private SafDeviceDatabaseRpcService proxy;

    @Override
    public boolean lock(String device, String id) {
        LOG.info("Trying to lock device '{}'", device);
        return proxy.deviceLock(DeviceLockInput.builder().entity(device).lockId(id).lockState(true).build())
                .getSuccess();
    }

    @Override
    public boolean unlock(String device, String id) {
        LOG.info("Trying to unlock device '{}'", device);
        return proxy.deviceLock(DeviceLockInput.builder().entity(device).lockId(id).lockState(false).build())
                .getSuccess();
    }
}