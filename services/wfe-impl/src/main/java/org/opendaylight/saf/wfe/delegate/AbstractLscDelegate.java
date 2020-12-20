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
import com.google.gson.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.wfe.impl.model.RestconfDataRequest;
import org.opendaylight.saf.wfe.impl.model.RestconfDataResult;
import org.opendaylight.saf.wfe.util.DelegateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractLscDelegate extends AbstractDelegate implements JavaDelegate {
    @SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLscDelegate.class);
    private static final TypeToken<List<RestconfDataRequest>> TT = new TypeToken<>() {
    };

    private final String operation;

    AbstractLscDelegate(String operation) {
        this.operation = operation;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        final List<RestconfDataRequest> items = getInput(execution, TT);
        LOG.info("{} input {}", operation, items);
        final List<RestconfDataResult> output = items.stream()
                .map(this::fixYangPath)
                .map(this::mapChecked)
                .collect(Collectors.toList());
        LOG.info("{} output {}", operation, output);
        execution.getProcessInstance().setVariable(execution.getProcessInstanceId(), output);
    }

    private RestconfDataRequest fixYangPath(RestconfDataRequest in) {
        return RestconfDataRequest.builder()
                .configData(in.getConfigData())
                .yangPath(DelegateUtils.getEncodedPath(Optional.ofNullable(in.getYangPath()).orElse("")))
                .build();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private RestconfDataResult mapChecked(RestconfDataRequest input) {
        try {
            Objects.requireNonNull(input, "Invalid input");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(input.getYangPath()), "YANG path is missing");
            return mappingFunction(input);
        } catch (Exception e) {
            return failedResult(operation + " failed", e);
        }
    }

    @SuppressFBWarnings("SLF4J_FORMAT_SHOULD_BE_CONST")
    protected static RestconfDataResult failedResult(String message, Exception cause) {
        final String msg = message + " : " + cause.getMessage();
        LOG.warn(msg);
        return RestconfDataResult.builder().success(false).error(msg).build();
    }

    protected abstract RestconfDataResult mappingFunction(RestconfDataRequest input) throws IOException;
}
