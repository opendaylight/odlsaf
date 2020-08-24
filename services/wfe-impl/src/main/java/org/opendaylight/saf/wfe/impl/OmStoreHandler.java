/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryTimeoutException;
import org.opendaylight.jsonrpc.model.AddListenerArgument;
import org.opendaylight.jsonrpc.model.DataOperationArgument;
import org.opendaylight.jsonrpc.model.DeleteListenerArgument;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.StoreOperationArgument;
import org.opendaylight.jsonrpc.model.TxArgument;
import org.opendaylight.jsonrpc.model.TxOperationArgument;
import org.opendaylight.saf.springboot.annotation.Responder;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.StatusInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.StatusOutput;
import org.opendaylight.saf.wfe.util.DelegateConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Service implementation of OmShard interface.
 *
 * @author Pravin Kumar Damodaran
 *
 */
@Component
@ConditionalOnProperty("data-endpoint")
@Responder("${data-endpoint}")
public class OmStoreHandler implements RemoteOmShard {
    private static final Logger LOG = LoggerFactory.getLogger(OmStoreHandler.class);
    private static final String STORE = "store";
    private static final String ENTITY = "entity";
    private static final String PATH = "path";
    private static final String DATA = "data";
    private static final String OPERATION = "operation";
    private static final Integer TIMER = 5000;

    private Map<String, Boolean> transactionStatusMap = new HashMap<>();
    private Map<String, String> transactionErrorMap = new HashMap<>();

    @Autowired
    private WorkflowEngineHandler wfeHandler;

    @Autowired
    private RepositoryService repositoryService;

    /**
     * Returns a transaction id to be used by LSC for put and delete method calls. Not to be confused with the job-id
     * returned by wfe
     */
    @Override
    public String txid() {
        return UUID.randomUUID().toString();
    }


    /**Execute workflow with the given arguments as inputs and return the response.
     *
     * @param method PUT, GET or DELETE. Used to generate the workflow name
     * @param store Config or Operational
     * @param entity Name of the entity (device/south bound controller)
     * @param path Yang path for which the request is made
     * @param data Data to be configured on the southbound element (not null only for PUT request, null for others)
     * @return Workflow response
     */
    private JsonElement executeWorkflow(String method, String store, String entity,
            JsonElement path, @Nullable JsonElement data) {
        String workflowKey = path.getAsJsonObject().keySet().iterator().next();
        String workflowName = generateWorkflowName(method, workflowKey);
        LOG.info("Workflow name: {}", workflowName);
        JsonObject input = new JsonObject();
        input.addProperty(STORE, store);
        input.addProperty(ENTITY, entity);
        input.add(PATH, path);
        if (data != null) {
            input.add(DATA, data);
        }

        // Pass method name for default workflow (for now, pass it to all workflows)
        input.addProperty(OPERATION, method);
        JsonArray inputArray = new JsonArray();
        inputArray.add(input);
        ExecuteOutput output = wfeHandler.execute(new ExecuteInput(workflowName, inputArray));
        String jobId = output.getJobId();
        LOG.info("Job id:{}", jobId);
        return waitForJob(jobId);
    }

    private JsonElement waitForJob(String jobId) {
        // wait for TIMER * 20 milliseconds for status to become COMPLETED.
        // If not completed by then, raise TimeoutException
        for (int i = 1;i < 20;i++) {
            StatusOutput statusOutput = wfeHandler.status(StatusInput.builder().jobId(jobId).build());

            LOG.info("Status output : {}", statusOutput);
            if ("COMPLETED".equals(statusOutput.getWorkflowStatus())) {
                return statusOutput.getWorkflowOutput();
            } else if ("INTERNALLY_TERMINATED".equals(statusOutput.getWorkflowStatus())) {
                throw new IllegalStateException("Workflow internally terminated. Check logs");
            }
            Uninterruptibles.sleepUninterruptibly(TIMER, TimeUnit.MILLISECONDS);
        }
        throw new MessageLibraryTimeoutException(String.format("Workflow not completed within %d seconds",
                (TIMER * 20 / 1000)));
    }


    /**
     * Generates workflow name using the given method and yang path key.
     *
     * @param method GET, PUT or DELETE
     * @param workflowKey Yang path key for which the workflow needs to be executed
     * @return Generated Workflow name.Will be of the format method_yangpathkey. Eg, for Get on path
     *         openconfig-local-routing:static-routes, the generated workflow name will be
     *         <b>GET_openconfig_local_routing_static_routes</b><br>
     *         If the workflow is not found, then default workflow names will be returned based on the following rules:
     *         <ul>
     *         <li>DEFAULT_yangPathKey will be returned if present.
     *              Eg: <b>DEFAULT_openconfig_local_routing_static_routes</b></li>
     *         <li>Else, method_DEFAULT will be returned if present.
     *              Eg: <b>GET_DEFAULT</b></li>
     *         <li>Else, <b>DEFAULT_WORKFLOW</b> will be returned.</li>
     *         </ul>
     */
    public String generateWorkflowName(String method, String workflowKey) {
        LOG.info("Yang path Key: {}", workflowKey);
        String workflowName = String.format("%s_%s", method, workflowKey.replace(":", "_").replace("-", "_"));
        String defaultYangpathWorkflowName = String.format("DEFAULT_%s",  workflowKey.replace(":", "_")
                .replace("-", "_"));
        String defaultMethodWorkflowName = String.format("%s_DEFAULT", method);
        List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery().latestVersion()
                .list();
        LOG.info("ProcessDefinitionList:{}", processDefinitionList);
        if (processDefinitionList.stream().anyMatch(processDefinition ->
                workflowName.equals(processDefinition.getName()))) {
            return workflowName;
        } else if (processDefinitionList.stream().anyMatch(processDefinition ->
                defaultYangpathWorkflowName.equals(processDefinition.getName()))) {
            LOG.info("{} not found", workflowName);
            return defaultYangpathWorkflowName;
        } else if (processDefinitionList.stream().anyMatch(processDefinition ->
                defaultMethodWorkflowName.equals(processDefinition.getName()))) {
            LOG.info("{}, {} not found", workflowName, defaultYangpathWorkflowName);
            return defaultMethodWorkflowName;
        }
        LOG.info("{}, {}, {} not found", workflowName, defaultYangpathWorkflowName, defaultMethodWorkflowName);
        return "DEFAULT_WORKFLOW";
    }


    /**
     * Update transaction status map. If workflow is not success, update transaction error map with the error message.
     * If workflow response does not have "success" json key, update transaction status as false.
     * @param txId Transaction id
     * @param workflowResponse Workflow response
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private void updateTransactionStatus(String txId, JsonElement workflowResponse) {
        try {
            JsonElement successElement = workflowResponse.getAsJsonObject().get(DelegateConstants.SUCCESS);
            Preconditions.checkNotNull(successElement, "Success key missing in workflow response. "
                    + "Unable to say whether request was success or not");
            Boolean workflowStatus = successElement.getAsBoolean();
            transactionStatusMap.put(txId, workflowStatus);
            if (!workflowStatus) {
                JsonElement errorElement = workflowResponse.getAsJsonObject().get(DelegateConstants.ERROR);
                Preconditions.checkNotNull(errorElement, "No error message in workflow response");
                transactionErrorMap.put(txId, errorElement.getAsString());
            }
        } catch (Exception e) {
            transactionStatusMap.put(txId, false);
            transactionErrorMap.put(txId, e.getMessage());
        }
    }


    /**
     * Calls the executeWorkflow method and returns the response.
     */
    @Override
    public JsonElement read(StoreOperationArgument arg) {
        JsonElement workflowResponse = executeWorkflow(DelegateConstants.GET_REQUEST, arg.getStore(), arg.getEntity(),
                arg.getPath(), null);

        return workflowResponse;
    }

    /**
     * Calls the executeWorkflow method and updates the response in transaction status map.
     * If the workflow is success, response should be true. Else, response should be false. The response of
     * PUT workflow should have a boolean field "success" to check and update the status.
     * If success is false, the response should have "error" field to get and update the transaction-Error Map
     */
    @Override
    public void put(DataOperationArgument arg) {
        JsonElement workflowResponse = executeWorkflow(DelegateConstants.PUT_REQUEST, arg.getStore(), arg.getEntity(),
                arg.getPath(), arg.getData());
        updateTransactionStatus(arg.getTxid(), workflowResponse);
    }

    /**
     * Returns true for all requests.
     */
    @Override
    public boolean exists(StoreOperationArgument arg) {
        return true;
    }


    @Override
    public void merge(DataOperationArgument arg) {
        // do nothing. This method is called internally during PUT Restconf call. Can be ignored safely
    }


    /**
     * Calls the executeWorkflow method and updates the response in transaction status map.
     * If the workflow is success, response should be true. Else, response should be false. The response of
     * DELETE workflow should have a boolean field "success" to check and update the status.
     * If success is false, the response should have "error" field to get and update the transaction-Error Map
     */
    @Override
    public void delete(TxOperationArgument arg) {
        JsonElement workflowResponse = executeWorkflow(DelegateConstants.DELETE_REQUEST, arg.getStore(),
                arg.getEntity(), arg.getPath(), null);
        updateTransactionStatus(arg.getTxid(), workflowResponse);
    }

    /**
     * Gets the status (true/false) of the given txId from transaction Status map.
     */
    @Override
    public boolean commit(TxArgument arg) {
        return transactionStatusMap.get(arg.getTxid());
    }

    @Override
    public boolean cancel(TxArgument arg) {
        return false;
    }

    /**
     * Gets the error of the given txId from transaction error map. LSC expects a list, hence wrap the
     * error message as string in a list. This method is triggered by LSC when commit returns false.
     */
    @Override
    public List<String> error(TxArgument arg) {
        List<String> errorList = new ArrayList<>();
        errorList.add(transactionErrorMap.get(arg.getTxid()));
        return errorList;
    }

    @Override
    public ListenerKey addListener(AddListenerArgument arg) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }


    @Override
    public boolean deleteListener(DeleteListenerArgument arg) {
        throw new UnsupportedOperationException("Not implemented");
    }


    @Override
    public void close() {
        //NOOP
    }
}
