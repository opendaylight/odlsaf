/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.check;

import org.opendaylight.saf.wfe.impl.model.CheckArg;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.impl.model.RestconfClient;
import org.opendaylight.saf.wfe.util.DelegateUtils;

public class VerifyPutCheck extends AbstractCheck {

    public VerifyPutCheck(RestconfClient lscClient) {
        super(lscClient);
    }

    @Override
    public boolean test(CheckArg arg) {
        final HttpResponse response = callRestconf(arg.getYangPath());
        if (!DelegateUtils.isHttpOk(response.getCode())) {
            LOG.warn("Path invalid or not present : {}", arg.getYangPath());
            return false;
        }
        // TODO : This is missing implementation
        return response.getResponse().contains("null");
    }
}
