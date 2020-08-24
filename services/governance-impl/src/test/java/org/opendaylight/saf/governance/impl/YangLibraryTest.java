/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.FindInput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.FindOutput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.ListOutput;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.DependsInput;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.DependsOutput;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.SourceInput;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.SourceOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class YangLibraryTest extends AbstractGovernanceTest {
    private static final Logger LOG = LoggerFactory.getLogger(YangLibraryTest.class);

    @Test
    public void testFind() {
        FindOutput findOut = handler.find(FindInput.builder().module("ietf-inet-types").revision("2013-07-15").build());
        LOG.info("Find output : {}", findOut);
        assertThat(findOut.getModules().getAsJsonArray().size(), greaterThan(0));
    }

    @Test
    public void testList() {
        ListOutput listOut = handler.list();
        LOG.info("List output : {}", listOut);
        assertThat(listOut.getModules().getAsJsonArray().size(), greaterThan(1));
    }

    @Test
    public void testDepends() {
        DependsOutput depends = handler.depends(DependsInput.builder().module("jsonrpc").build());
        LOG.info("Dependencies {}", depends);
        assertThat(depends.getModules().getAsJsonArray().size(), greaterThan(1));
    }

    @Test
    public void testSource() {
        SourceOutput srcOut = handler
                .source(SourceInput.builder().module("ietf-inet-types").revision("2013-07-15").build());
        LOG.info("Source output : {}", srcOut);
        assertNotNull(srcOut.getSource());
    }

    @Test
    public void testNonExistentSource() {
        SourceOutput srcOut = handler.source(SourceInput.builder().module(UUID.randomUUID().toString()).build());
        LOG.info("Source output : {}", srcOut);
        assertNull(srcOut.getSource());
    }
}
