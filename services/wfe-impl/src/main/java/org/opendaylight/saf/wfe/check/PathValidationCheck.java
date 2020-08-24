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

/**
 * {@link AbstractCheck} that to verify that data exists at given path.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Aug 6, 2019
 */
public class PathValidationCheck extends AbstractCheck {
    public PathValidationCheck(RestconfClient lscClient) {
        super(lscClient);
    }

    @Override
    public boolean test(CheckArg arg) {
        final HttpResponse response = callRestconf(arg.getYangPath());
        if (!DelegateUtils.isHttpOk(response.getCode())) {
            LOG.warn("Path invalid or not present : {}", arg.getYangPath());
            return false;
        }
        return true;
    }
}
