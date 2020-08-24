/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import static org.opendaylight.saf.devicedb.impl.Util.SELF_NAME;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.opendaylight.saf.governance.client.GovernanceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeviceDbRegistration {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceDbRegistration.class);

    @Autowired
    private GovernanceClient client;

    @Value("${advertise-operations}")
    private String selfUri;

    @PostConstruct
    public void init() {
        LOG.info("Register self to {}", selfUri);
        client.set(SELF_NAME, selfUri);
        client.set(SELF_NAME, "-1", selfUri);
    }

    @PreDestroy
    public void close() {
        client.unset(SELF_NAME);
        client.unset(SELF_NAME, "-1");
    }
}
