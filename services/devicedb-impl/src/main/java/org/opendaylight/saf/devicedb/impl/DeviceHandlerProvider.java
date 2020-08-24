/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.saf.devicedb.api.saf_device_database.gen.rev20160608.devices.device.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory to get handler endpoint for given device.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Mar 7, 2020
 */
@Component
public class DeviceHandlerProvider extends AbstractDeviceHelper {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceHandlerProvider.class);
    private static final Set<String> CLI_ENGINE_TYPES = ImmutableSet.<String>of("icx", "mlx");
    private static final Set<String> NAPALM_ENGINE_TYPES = ImmutableSet.<String>of("eos", "junos", "nxos", "ios",
            "ios-xr");

    @Value("${napalm}")
    private String napalm;

    @Value("${cliengine}")
    private String cliengine;

    /**
     * Get device datastore handler for given device.
     *
     * @param deviceName device to find datastore handler for.
     * @return Device handler endpoint
     * @throws UnsupportedOperationException if device type is not supported
     */
    public String get(String deviceName) {
        final Device device = readDevice(deviceName, LogicalDatastoreType.CONFIGURATION, Device.class)
                .orElseThrow(() -> new IllegalStateException("Can't find device '" + deviceName + "'"));
        final String endpoint;
        if (CLI_ENGINE_TYPES.contains(device.getDeviceType())) {
            endpoint = cliengine;
        } else if (NAPALM_ENGINE_TYPES.contains(device.getDeviceType())) {
            endpoint = napalm;
        } else {
            throw new UnsupportedOperationException(
                    "Handling of device type '" + device.getDeviceType() + "' is not supported");
        }
        LOG.info("Device '{}' of type '{}' will be handled by '{}'", device.getEntity(), device.getDeviceType(),
                endpoint);
        return endpoint;
    }
}
