/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import static org.opendaylight.saf.wfe.util.DelegateConstants.PUT_REQUEST;
import static org.opendaylight.saf.wfe.util.DelegateConstants.RESTCONF_BASE;

import com.google.common.base.Preconditions;
import java.io.IOException;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.impl.model.RestconfDataRequest;
import org.opendaylight.saf.wfe.impl.model.RestconfDataResult;
import org.opendaylight.saf.wfe.util.DelegateUtils;
import org.springframework.stereotype.Component;

/**
 * {@link JavaDelegate} to perform data PUT into LSC datastore.
 *
 * <p>
 * Input parameters:
 * <ul>
 * <li>yang-path - path to data to put relative to "/restconf/" URI</li>
 * <li>config-data - actual data</li>
 * </ul>
 *
 * @author spichandi
 *
 */
@Component
public class PutDelegate extends AbstractLscDelegate {
    public PutDelegate() {
        super(PUT_REQUEST);
    }

    @Override
    protected RestconfDataResult mappingFunction(RestconfDataRequest input) throws IOException {
        Preconditions.checkNotNull(input.getConfigData(), "Configuration data are missing");
        HttpResponse response = client.call(PUT_REQUEST, RESTCONF_BASE + input.getYangPath(),
                input.getConfigData().toString());

        final boolean ok = DelegateUtils.isHttpOk(response.getCode());
        String responseObj = response.getResponse();
        if (responseObj.equals("{}")) {
            throw new IllegalArgumentException("Data not found: Invalid value in Yang Path : " + input.getYangPath());
        } else if (responseObj.contains("Message was not received within")) {
            response.setResponse("Request timed out.");
        } else if (responseObj.contains("connection failed")) {
            response.setResponse("Device is not reachable");
        }
        return RestconfDataResult.builder()
                .success(ok)
                .yangPath(input.getYangPath())
                .error(ok ? null : response.getResponse())
                .content(ok ? parser.parse(response.getResponse()) : null)
                .build();
    }
}