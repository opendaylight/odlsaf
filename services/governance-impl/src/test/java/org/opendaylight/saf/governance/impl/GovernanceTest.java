/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.GovernanceInput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.GovernanceOutput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SetGovernanceInput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.UnsetGovernanceInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class GovernanceTest extends AbstractGovernanceTest {
    private static final Logger LOG = LoggerFactory.getLogger(GovernanceTest.class);

    @Test
    public void testGovernance() {
        // configure governance for devicedb
        handler.setGovernance(SetGovernanceInput.builder()
                .entity("devicedb")
                .path(new JsonObject())
                .store("config")
                .uri("zmq://127.0.0.1:12000")
                .build());
        // check that it exists
        GovernanceOutput out = handler.governance(
                GovernanceInput.builder().entity("devicedb").store("config").path(new JsonObject()).build());
        LOG.info("Governance output : {}", out);
        assertNotNull(out.getUri());
        // remove
        handler.unsetGovernance(
                UnsetGovernanceInput.builder().entity("devicedb").store("config").path(new JsonObject()).build());

        out = handler.governance(
                GovernanceInput.builder().entity("devicedb").store("config").path(new JsonObject()).build());
        // make sure it's gone
        LOG.info("Governance output : {}", out);
        assertNull(out.getUri());
    }
}
