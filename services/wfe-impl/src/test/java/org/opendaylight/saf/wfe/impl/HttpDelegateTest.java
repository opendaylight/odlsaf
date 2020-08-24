/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.saf.wfe.util.TestConstants.PARSER;
import static org.opendaylight.saf.wfe.util.TestUtils.readResourceAsText;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.jsonrpc.bus.messagelib.TestHelper;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExecuteOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { WFEApplication.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HttpDelegateTest extends AbstractWorkflowTest {
    private static Server server;
    private static int port;

    @Autowired
    private WorkflowEngineHandler handler;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        port = TestHelper.getFreeTcpPort();
        server = new Server(port);
        final ContextHandler handler = new ContextHandler() {
            @Override
            public void doHandle(String target, Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response) throws IOException, ServletException {
                baseRequest.setMethod("GET");
                super.doHandle(target, baseRequest, request, response);
            }
        };
        final ResourceHandler rh = new ResourceHandler();
        rh.setBaseResource(Resource.newClassPathResource("/"));
        handler.setContextPath("/");
        handler.setHandler(rh);
        server.setHandler(handler);
        server.start();
    }

    @AfterClass
    public static void teardownAfterClass() throws Exception {
        server.stop();
    }

    private JsonObject getInput(String method, String uri) {
        final JsonObject ret = (JsonObject) PARSER.parse(readResourceAsText("http-req.json"));
        final JsonObject obj = ret.get("workflow-input").getAsJsonArray().get(0).getAsJsonObject();
        obj.add("method", new JsonPrimitive(method));
        obj.add("uri", new JsonPrimitive("http://127.0.0.1:" + port + "/" + uri));
        return ret;
    }

    @Test
    public void testNonExistent() throws IOException {
        ExecuteOutput job = handler.execute(ExecuteInput.builder()
                .workflowName("Http")
                .workflowInput(getInput("GET", "non/existent/path").getAsJsonObject().get("workflow-input"))
                .build());
        JsonElement out = waitForJob(handler, job.getJobId());
        assertEquals(404, out.getAsJsonArray().get(0).getAsJsonObject().get("code").getAsInt());
    }

    @Test
    public void testPost() {
        ExecuteOutput job = handler.execute(ExecuteInput.builder()
                .workflowName("Http")
                .workflowInput(getInput("POST", "get-multi.json").getAsJsonObject().get("workflow-input"))
                .build());
        JsonElement out = waitForJob(handler, job.getJobId());
        assertEquals(200, out.getAsJsonArray().get(0).getAsJsonObject().get("code").getAsInt());

    }

    @Test
    public void testFormData() {
        final JsonArray input = getInput("POST", "get-multi.json").getAsJsonObject()
                .get("workflow-input")
                .getAsJsonArray();
        input.get(0)
                .getAsJsonObject()
                .get("headers")
                .getAsJsonObject()
                .add(HttpHeaders.CONTENT_TYPE, new JsonPrimitive("application/x-www-form-urlencoded"));
        JsonObject body = new JsonObject();
        body.add("abc", new JsonPrimitive("123"));
        input.get(0).getAsJsonObject().add("data", body);
        ExecuteOutput job = handler.execute(ExecuteInput.builder().workflowName("Http").workflowInput(input).build());
        JsonElement out = waitForJob(handler, job.getJobId());
        assertEquals(200, out.getAsJsonArray().get(0).getAsJsonObject().get("code").getAsInt());
    }
}
