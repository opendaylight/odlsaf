/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.saf.wfe.util.TestConstants.PARSER;
import static org.opendaylight.saf.wfe.util.TestUtils.readResourceAsText;

import com.google.gson.JsonArray;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.SafPlasticRpcService;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.TranslateInput;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.TranslateOutput;
import org.opendaylight.saf.springboot.annotation.RequesterProxy;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ListInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ListOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SafWfeRpcService;
import org.opendaylight.saf.wfe.impl.model.HttpClient;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.impl.model.LockService;
import org.opendaylight.saf.wfe.impl.model.PlasticClient;
import org.opendaylight.saf.wfe.impl.model.RestconfClient;
import org.opendaylight.saf.wfe.util.DelegateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { WFEApplication.class })
@TestPropertySource(properties = { "napalm=zmq://127.0.0.1:14000", "plastic=zmq://127.0.0.1:14001",
        "devicedb=zmq://127.0.0.1:14001", "data-endpoint=zmq://127.0.0.1:14001" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkflowEngineHandlerTest extends AbstractWorkflowTest {
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowEngineHandlerTest.class);
    @MockBean
    private RestconfClient lscClient;

    @MockBean
    private LockService lockService;

    @MockBean
    private PlasticClient caasClient;

    @MockBean
    private HttpClient httpClient;

    @Autowired
    private TransportFactory transportFactory;

    @RequesterProxy("${endpoint}")
    private SafWfeRpcService proxy;

    @Value("${workspace}")
    private Path workspace;

    @Before
    public void setUp() throws URISyntaxException {
        DelegateUtils.awaitForProxy(transportFactory, proxy, 2000L);
        when(lockService.lock(anyString(), anyString())).thenReturn(true);
        when(lockService.unlock(anyString(), anyString())).thenReturn(true);
    }

    @After
    public void tearDown() {
        Mockito.reset(lscClient, lockService);
    }

    @Test(timeout = 60_000)
    public void testExecuteGet() throws IOException {
        when(lscClient.call(eq("GET"), anyString(), eq(null)))
                .thenReturn(HttpResponse.builder().code(200).response("{ \"mock_key\" : \"mock_value\"}").build());

        final ExecuteOutput execOutput = proxy.execute(ExecuteInput.builder()
                .workflowName("GET")
                .workflowInput(
                        PARSER.parse(readResourceAsText("get-multi.json")).getAsJsonObject().get("workflow-input"))
                .build());

        LOG.info("Execute output : {}", execOutput);

        ListOutput output = proxy.list(ListInput.builder().filter("GET").build());

        final JsonArray result = waitForJob(proxy, execOutput.getJobId()).getAsJsonArray();
        assertEquals(2, result.size());
        assertTrue(result.get(0).getAsJsonObject().get("success").getAsBoolean());

        output = proxy.list(ListInput.builder().filter("GET").build());
        LOG.info("Search output : {}", output);
        assertThat(output.getWorkflowInstances().size(), not(lessThan(1)));
        assertEquals("GET", output.getWorkflowInstances().get(0).getProcessDefinitionKey());
    }

    @Test(timeout = 60_000)
    public void testExecutePut() throws IOException {
        when(lscClient.call(eq("PUT"), anyString(), anyString()))
                .thenReturn(HttpResponse.builder().code(200).response("").build());
        when(lscClient.call(eq("GET"), anyString(), eq(null)))
                .thenReturn(HttpResponse.builder().code(200).response("").build());

        final ExecuteOutput execOutput = proxy.execute(ExecuteInput.builder()
                .workflowName("PUT")
                .workflowInput(PARSER.parse(readResourceAsText("PUT-req.json")).getAsJsonObject().get("workflow-input"))
                .build());

        LOG.info("Execute output : {}", execOutput);
        waitForJob(proxy, execOutput.getJobId());

        final ExecuteOutput execOutput2 = proxy.execute(ExecuteInput.builder()
                .workflowName("PUT")
                .workflowInput(
                        PARSER.parse(readResourceAsText("PUT-req2.json")).getAsJsonObject().get("workflow-input"))
                .build());

        LOG.info("Execute output : {}", execOutput);
        waitForJob(proxy, execOutput2.getJobId());
    }

    @Test(timeout = 60_000)
    public void testExecuteDelete() throws IOException {
        when(lscClient.call(eq("DELETE"), anyString(), eq(null)))
                .thenReturn(HttpResponse.builder().code(200).response("").build());
        when(lscClient.call(eq("GET"), anyString(), eq(null)))
                .thenReturn(HttpResponse.builder().code(200).response("").build());

        final ExecuteOutput execOutput = proxy.execute(ExecuteInput.builder()
                .workflowName("DELETE")
                .workflowInput(
                        PARSER.parse(readResourceAsText("DELETE-req.json")).getAsJsonObject().get("workflow-input"))
                .build());

        LOG.info("Execute output : {}", execOutput);
        waitForJob(proxy, execOutput.getJobId());
    }

    @Test
    public void testExecuteTranslate() {
        SafPlasticRpcService service = mock(SafPlasticRpcService.class);
        when(service.translate(any(TranslateInput.class))).thenReturn(TranslateOutput.builder().data("1234").build());
        when(caasClient.getService()).thenReturn(service);
        final ExecuteOutput execOutput = proxy.execute(ExecuteInput.builder()
                .workflowName("TRANSLATE")
                .workflowInput(
                        PARSER.parse(readResourceAsText("caas-req.json")).getAsJsonObject().get("workflow-input"))
                .build());
        final String result = waitForJob(proxy, execOutput.getJobId()).getAsJsonPrimitive().getAsString();
        assertEquals("1234", result);
        verify(caasClient, times(1)).getService();
        verify(service, times(1)).translate(any(TranslateInput.class));
    }
}
