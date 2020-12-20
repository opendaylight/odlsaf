/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base generator class.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 11, 2019
 */
abstract class AbstractGenerator {
    @SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractGenerator.class);

    protected static void writeTemplate(VelocityContext context, File file, String template) throws IOException {
        LOG.debug("Writing file {}", file);
        try (InputStream is = new ByteArrayInputStream(
                Util.renderTemplate(template, context).getBytes(StandardCharsets.UTF_8));
                OutputStream os = Files.newOutputStream(file.toPath())) {
            is.transferTo(os);
        }
    }

    protected final Module module;
    protected final File baseDir;
    protected final VelocityContext baseContext;

    AbstractGenerator(File baseDir, Module module, VelocityContext baseContext) {
        this.baseDir = Objects.requireNonNull(baseDir);
        this.module = Objects.requireNonNull(module);
        this.baseContext = Objects.requireNonNull(baseContext);
    }

    public abstract List<File> generate() throws IOException;
}
