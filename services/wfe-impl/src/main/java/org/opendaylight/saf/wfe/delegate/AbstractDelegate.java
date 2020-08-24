/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.opendaylight.saf.wfe.impl.model.RestconfClient;
import org.opendaylight.saf.wfe.util.DelegateConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(name = "restconf")
abstract class AbstractDelegate {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDelegate.class);
    @Autowired
    protected RestconfClient client;

    @Autowired
    protected Gson gson;

    @Autowired
    protected JsonParser parser;

    protected JsonArray ensureList(Object data) {
        JsonArray result = null;
        if (data instanceof JsonArray) {
            result = (JsonArray) data;
        }
        else if (data instanceof JsonObject) {
            JsonArray wrapper = new JsonArray(1);
            wrapper.add((JsonObject) data);
            result = wrapper;
        }
        return result;
    }

    protected <T> List<T> getInput(DelegateExecution execution, TypeToken<List<T>> type) {
        final Object workflowInput = execution.getVariable(DelegateConstants.WORKFLOW_INPUT);
        RuntimeService taskService = execution.getProcessEngineServices().getRuntimeService();
        if (workflowInput != null && (workflowInput instanceof JsonArray || workflowInput instanceof JsonObject)) {
            return gson.fromJson(ensureList(workflowInput), type.getType());
        } else if (taskService.getVariables(execution.getId()) != null) {
            LOG.info("Variables {}", taskService.getVariables(execution.getId()).get("task"));
            Map<String, Object> variablesMap = taskService.getVariables(execution.getId());
            JsonArray value = parser.parse(variablesMap.get("task").toString()).getAsJsonArray();
            return gson.fromJson(value, type.getType());
        }
        return Collections.emptyList();
    }
}