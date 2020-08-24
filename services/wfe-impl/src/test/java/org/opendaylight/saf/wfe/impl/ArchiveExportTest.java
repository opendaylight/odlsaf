/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.saf.wfe.util.TestUtils.copyResource;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.saf.springboot.annotation.RequesterProxy;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExportInput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.ExportOutput;
import org.opendaylight.saf.wfe.api.saf_wfe.gen.rev20190214.SafWfeRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { WFEApplication.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ArchiveExportTest extends AbstractWorkflowTest {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveExportTest.class);
    @Value("${workspace}")
    private Path workspace;

    @RequesterProxy("${endpoint}")
    private SafWfeRpcService proxy;

    @After
    public void tearDown() throws IOException {
        Files.walk(workspace.getParent().resolve("exports"))
                .filter(path -> path.getFileName().toString().startsWith("WFDsArchive_"))
                .forEach(ArchiveExportTest::deleteUnchecked);

    }

    @Test
    public void testExportWithFilter() throws IOException {
        copyResource("wf1/wf1-script1.py", workspace);
        copyResource("wf1/wf1-script2.py", workspace);
        copyResource("wf1/wf1.bpmn", workspace);

        // await for deployment to be ready
        assertTrue(awaitForProcessDefinition(30, "wf1", true));
        // this looks ugly, but I'm not aware of better method - process definition is available,
        // but for some reason is not yet fully ready for export. Camunda "magic" ...
        Uninterruptibles.sleepUninterruptibly(500L, TimeUnit.MILLISECONDS);
        assertTrue(awaitForProcessDefinition(30, "wf1", true));

        // try empty filter first, which is equal to regex ".*"
        ExportOutput out = proxy.export(new ExportInput("", true));

        List<String> content = getZipContent(workspace.getParent().resolve("exports").resolve(out.getFilename()));
        LOG.info("Content : {}", content);
        assertThat(content.size(), greaterThan(3));
        assertThat(content, hasItem("wf1.bpmn"));
        assertThat(content, hasItem("GET.bpmn"));

        // filter on deployment "LEAP-183"
        out = proxy.export(new ExportInput("wf1", false));

        content = getZipContent(workspace.getParent().resolve("exports").resolve(out.getFilename()));
        LOG.info("Content : {}", content);
        assertThat(content, hasSize(3));
        assertThat(content, hasItem("wf1.bpmn"));
        // Export non-existent process definition without built-in deployment
        out = proxy.export(new ExportInput(UUID.randomUUID().toString(), false));
        assertFalse(out.getSuccess());
    }

    private static List<String> getZipContent(Path zip) throws IOException {
        final List<String> result = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            for (;;) {
                final ZipEntry ze = zis.getNextEntry();
                if (ze == null) {
                    break;
                }
                result.add(ze.getName());
            }
        }
        return result;
    }

    private static void deleteUnchecked(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
