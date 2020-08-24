/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import static org.opendaylight.saf.codegen.Util.DTO;
import static org.opendaylight.saf.codegen.Util.FIELDS;
import static org.opendaylight.saf.codegen.Util.PROP_JAVA_NAME;
import static org.opendaylight.saf.codegen.Util.PROP_YANG_MOD;
import static org.opendaylight.saf.codegen.Util.getTargetFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.Module;

abstract class DataNodeContainerChildGenerator extends AbstractGenerator {

    DataNodeContainerChildGenerator(File baseDir, Module module, VelocityContext baseContext) {
        super(baseDir, module, baseContext);
    }

    protected void process(final List<File> result, final String basePackage, final VelocityContext context,
            String prefix, String suffix, DataNodeContainer node) {
        try {
            final String filename = new StringBuilder().append(Util.yang2javaClassName(prefix))
                    .append(suffix)
                    .toString();
            context.put(PROP_JAVA_NAME, filename);
            context.put(PROP_YANG_MOD, module.getQNameModule());
            final List<FieldInfo> fields = Util.collectFields(node, basePackage, child -> {
                try {
                    result.addAll(new DtoGenerator(baseDir, module, context, child).generate());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            context.put(FIELDS, fields);
            final File out = getTargetFile(baseDir, basePackage, filename);
            writeTemplate(context, out, DTO);
            result.add(out);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
