/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.SafPlasticRpcService;
import org.opendaylight.saf.springboot.annotation.RequesterProxy;
import org.opendaylight.saf.wfe.impl.model.PlasticClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "plastic")
@Component
public class PlasticClientImpl implements PlasticClient {
    @RequesterProxy("${plastic}")
    private SafPlasticRpcService proxy;

    @Override
    public SafPlasticRpcService getService() {
        return proxy;
    }
}
