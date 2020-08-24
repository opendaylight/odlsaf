/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.SafDeviceDatabaseRpcService;

/**
 * API to lock/unlock device in device database.
 *
 * @author spichandi
 * @see SafDeviceDatabaseRpcService
 */
public interface LockService {
    /**
     * Attempt to lock device in devicedb.If already locked, method will return false.
     *
     * @param device name of device to lock.
     * @param id lock id associated with workflow instance.
     * @return true if and only if device was successfully locked, false otherwise.
     */
    boolean lock(String device, String id);

    /**
     * Unlock given device in devicedb. If device was already unlocked, method return true.
     *
     * @param device name of device to unlock.
     * @param id lock id associated with workflow instance.
     * @return true if device was left in unlocked state after RPC finished.
     */
    boolean unlock(String device, String id);
}
