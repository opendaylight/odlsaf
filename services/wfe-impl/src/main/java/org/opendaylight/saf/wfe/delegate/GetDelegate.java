/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import static org.opendaylight.saf.wfe.util.DelegateConstants.GET_REQUEST;
import static org.opendaylight.saf.wfe.util.DelegateConstants.RESTCONF_BASE;

import java.io.IOException;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.impl.model.RestconfDataRequest;
import org.opendaylight.saf.wfe.impl.model.RestconfDataResult;
import org.opendaylight.saf.wfe.util.DelegateUtils;
import org.springframework.stereotype.Component;

/**
 * {@link JavaDelegate} to GET data from LSC datastore.
 *
 * <p>
 * Input parameters:
 * <ul>
 * <li>yang-path - path to data to GET relative to "/restconf/" URI</li>
 * </ul>
 *
 * @author spichandi
 *
 */
@Component
public class GetDelegate extends AbstractLscDelegate {
    public GetDelegate() {
        super(GET_REQUEST);
    }

    @Override
    protected RestconfDataResult mappingFunction(RestconfDataRequest input) throws IOException {
        final HttpResponse response = client.call(GET_REQUEST, RESTCONF_BASE + input.getYangPath(), null);
        final boolean ok = DelegateUtils.isHttpOk(response.getCode());
        return RestconfDataResult.builder()
                .success(ok)
                .yangPath(input.getYangPath())
                .error(ok ? null : response.getResponse())
                .content(ok ? parser.parse(response.getResponse()) : null)
                .build();
    }
}