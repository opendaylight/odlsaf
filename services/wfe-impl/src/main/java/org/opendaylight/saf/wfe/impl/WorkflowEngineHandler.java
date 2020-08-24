/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import static org.opendaylight.saf.wfe.util.DelegateUtils.removeArrayElement;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.opendaylight.saf.opentracing.support.TracingSupport;
import org.opendaylight.saf.springboot.annotation.Responder;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ActivateInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ActivateOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.CancelInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.CancelOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExportInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExportOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ListInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ListOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SafWfeRpcService;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.StatusInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.StatusOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SuspendInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SuspendOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.workflowInstances.WorkflowInstances;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.workflowInstances.WorkflowInstances.WorkflowInstancesBuilder;
import org.opendaylight.saf.wfe.util.DelegateConstants;
import org.opendaylight.saf.wfe.util.InputMapInjectAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service implementation of {@link SafWfeRpcService}.
 *
 * @author spichandi
 *
 */
@Component
@Responder("${endpoint}")
public class WorkflowEngineHandler implements SafWfeRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowEngineHandler.class);
    private static final Predicate<HistoricProcessInstance> MATCH_ALL_HPI = hpi -> true;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ArchiveExporter archiveExporter;

    @Autowired
    private Gson gson;

    @Value("${workspace}")
    private String workspace;

    @Value("${restconf-username}")
    private String username;

    @Value("${restconf-password}")
    private String password;

    @Value("${restconf}")
    private String endpoint;

    @Override
    public ExecuteOutput execute(ExecuteInput input) {
        final JsonElement workflowInput = removeArrayElement(input.getWorkflowInput());
        LOG.info("Execute input : {}", workflowInput);
        final Map<String, Object> inputMap = new HashMap<>();
        inputMap.put(DelegateConstants.WORKFLOW_INPUT, workflowInput);
        // for delegates that don't want to deal with JsonElement instances, like python
        inputMap.put(DelegateConstants.WORKFLOW_INPUT_STRING, workflowInput.toString());

        if (TracingSupport.isTracing()) {
            final Tracer tracer = TracingSupport.getTracer();
            final Span span = tracer.buildSpan("Execute/" + input.getWorkflowName()).start();
            tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new InputMapInjectAdapter(inputMap));
            try (Scope scope = tracer.scopeManager().activate(span)) {
                return executeInternal(input, inputMap);
            } finally {
                span.finish();
            }
        } else {
            return executeInternal(input, inputMap);
        }
    }

    private ExecuteOutput executeInternal(ExecuteInput input, final Map<String, Object> inputMap) {
        inputMap.put(DelegateConstants.WORKFLOW_NAME, input.getWorkflowName());
        inputMap.put(DelegateConstants.LSC_USERNAME, username);
        inputMap.put(DelegateConstants.LSC_PASSWORD, password);
        inputMap.put(DelegateConstants.LSC_HOSTNAME, endpoint);
        inputMap.put(DelegateConstants.USER_DATA, Paths.get(workspace).resolve("data").toAbsolutePath().toString());
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(input.getWorkflowName(), inputMap);
        return ExecuteOutput.builder().jobId(pi.getProcessInstanceId()).build();
    }

    @Override
    public StatusOutput status(StatusInput input) {
        final String state = getProcessInstanceStatus(input.getJobId());
        final Object data = state.equals("COMPLETED")
                ? historyService.createHistoricVariableInstanceQuery()
                        .variableName(input.getJobId())
                        .singleResult()
                        .getValue()
                : null;

        LOG.info("Status data : {}", data);
        return StatusOutput.builder().workflowStatus(state).workflowOutput(gson.toJsonTree(data)).build();
    }

    @Override
    public ListOutput list(ListInput input) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .orderByProcessInstanceStartTime()
                .asc();

        return ListOutput.builder()
                .workflowInstances(query.list()
                        .stream()
                        .filter(hpiListFilter(input))
                        .map(WorkflowEngineHandler::mapToWorkflowInstance)
                        .collect(Collectors.toList()))
                .build();
    }

    private static WorkflowInstances mapToWorkflowInstance(HistoricProcessInstance hpi) {
        final WorkflowInstancesBuilder builder = WorkflowInstances.builder();
        if (hpi.getStartTime() != null) {
            builder.startTime(hpi.getStartTime().toInstant().toString());
        } else {
            builder.startTime("");
        }
        if (hpi.getEndTime() != null) {
            builder.endTime(hpi.getEndTime().toInstant().toString());
        } else {
            builder.endTime("");
        }
        if (hpi.getDurationInMillis() != null) {
            builder.durationInMilliseconds(hpi.getDurationInMillis());
        } else {
            builder.durationInMilliseconds(0L);
        }
        return builder.state(hpi.getState())
                .id(hpi.getId())
                .processDefinitionId(hpi.getProcessDefinitionId())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .build();
    }

    private static Predicate<HistoricProcessInstance> hpiListFilter(ListInput filter) {
        if (filter == null || Strings.isNullOrEmpty(filter.getFilter())) {
            return MATCH_ALL_HPI;
        }
        return hpi -> Pattern.compile(filter.getFilter()).matcher(hpi.getProcessDefinitionKey()).find();
    }

    @Override
    public CancelOutput cancel(CancelInput input) {
        String piId = input.getJobId();
        runtimeService.deleteProcessInstance(piId, "Long running");
        return CancelOutput.builder().workflowStatus(getProcessInstanceStatus(piId)).build();
    }

    @Override
    public SuspendOutput suspend(SuspendInput input) {
        String piId = input.getJobId();
        runtimeService.suspendProcessInstanceById(piId);
        return SuspendOutput.builder().workflowStatus(getProcessInstanceStatus(piId)).build();
    }

    @Override
    public ActivateOutput activate(ActivateInput input) {
        String piId = input.getJobId();
        runtimeService.activateProcessInstanceById(piId);
        return ActivateOutput.builder().workflowStatus(getProcessInstanceStatus(piId)).build();
    }

    private String getProcessInstanceStatus(String piId) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(piId);
        HistoricProcessInstance pi = query.singleResult();
        Optional<HistoricProcessInstance> optionalPi = Optional.ofNullable(pi);
        if (optionalPi.isPresent()) {
            return pi.getState();
        } else {
            return "Matching Job Id is not present in Workflow Engine";
        }
    }

    @Override
    public ExportOutput export(ExportInput input) {
        return archiveExporter.export(input);
    }
}
