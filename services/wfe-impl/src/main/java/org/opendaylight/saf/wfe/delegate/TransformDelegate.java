/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import com.google.gson.JsonObject;
import java.util.Optional;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.TranslateInput;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.TranslateOutput;
import org.opendaylight.saf.wfe.impl.model.PlasticClient;
import org.opendaylight.saf.wfe.util.DelegateConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link JavaDelegate} that transform given data using CaaS.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jun 9, 2019
 */
@ConditionalOnProperty(name = "plastic")
@Component
public class TransformDelegate implements JavaDelegate {
    @Autowired
    private PlasticClient proxy;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        final JsonObject input = (JsonObject) execution.getVariable(DelegateConstants.WORKFLOW_INPUT);
        final TranslateOutput output = proxy.getService()
                .translate(TranslateInput.builder()
                        .inName(getVar(input, "in-name"))
                        .inType(getVar(input, "in-type"))
                        .inVersion(getVar(input, "in-version"))
                        .outName(getVar(input, "out-name"))
                        .outType(getVar(input, "out-type"))
                        .outVersion(getVar(input, "out-version"))
                        .data(getVar(input, "data"))
                        .build());
        execution.getProcessInstance().setVariable(execution.getProcessInstanceId(), output.getData());
    }

    private static String getVar(JsonObject input, String name) {
        return String.valueOf(Optional.ofNullable(input.get(name))
                .orElseThrow(() -> new IllegalStateException("Missing variable " + name)));
    }
}
