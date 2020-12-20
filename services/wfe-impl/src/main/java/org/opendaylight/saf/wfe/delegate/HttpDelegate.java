/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.wfe.impl.model.GenericHttpDataResponse;
import org.opendaylight.saf.wfe.impl.model.HttpClient;
import org.opendaylight.saf.wfe.impl.model.HttpDataRequest;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.util.DelegateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class HttpDelegate extends AbstractDelegate implements JavaDelegate {
    private static final Logger LOG = LoggerFactory.getLogger(HttpDelegate.class);
    private static final TypeToken<List<HttpDataRequest>> TT = new TypeToken<>() {
    };

    @Autowired
    private HttpClient httpClient;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        final List<HttpDataRequest> items = getInput(execution, TT);
        LOG.info("HTTP Data Request input {}", items);
        final List<GenericHttpDataResponse> output = items.stream().map(this::fixYangPath).map(this::mapChecked)
                .collect(Collectors.toList());
        LOG.info("HTTP Data Request output {}", output);
        if (!items.isEmpty() && items.get(0).getResponseVariableName() != null) {
            execution.getProcessInstance().setVariable(items.get(0).getResponseVariableName(), output);
        } else {
            execution.getProcessInstance().setVariable(execution.getProcessInstanceId(), output);
        }
    }

    private HttpDataRequest fixYangPath(HttpDataRequest requestInput) {
        requestInput.setUri(DelegateUtils.getEncodedPath(Optional.ofNullable(requestInput.getUri())
                .orElse("")));
        return requestInput;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private GenericHttpDataResponse mapChecked(HttpDataRequest input) {
        try {
            Objects.requireNonNull(input, "Invalid input");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(input.getUri()), "YANG path is missing");
            return mappingFunction(input);
        } catch (Exception e) {
            return failedResult("HTTP Data Request failed", e);
        }
    }

    private static GenericHttpDataResponse failedResult(String message, Exception cause) {
        final String msg = message + " : " + cause.getMessage();
        LOG.warn(msg);
        return GenericHttpDataResponse.builder().error(msg).build();
    }

    private GenericHttpDataResponse mappingFunction(HttpDataRequest input) throws IOException {
        LOG.info("HttpDataReq {}", input);
        final Map<String, String> headers = gson
                .fromJson(Optional.ofNullable(input.getHeaders()).orElse(new JsonObject()), DelegateUtils.MAP_TYPE);
        final HttpResponse response = httpClient.call(input.getMethod(), input.getUri(), input.getConnectionTimeout(),
                input.getReadTimeout(), headers, input.getData() != null ? input.getData().toString() : null);
        LOG.info("HttpResponse {}", response);

        return GenericHttpDataResponse.builder().content(response.getResponse()).code(response.getCode()).build();
    }
}