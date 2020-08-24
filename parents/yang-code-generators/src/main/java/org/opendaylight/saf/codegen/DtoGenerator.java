/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import static org.opendaylight.saf.codegen.Util.PROP_JAVA_NAME;
import static org.opendaylight.saf.codegen.Util.PROP_JAVA_PACKAGE;
import static org.opendaylight.saf.codegen.Util.PROP_YANG_MOD;
import static org.opendaylight.saf.codegen.Util.collectFields;
import static org.opendaylight.saf.codegen.Util.getTargetFile;
import static org.opendaylight.saf.codegen.Util.yang2fieldName;
import static org.opendaylight.saf.codegen.Util.yang2javaClassName;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Generator which emits DTO for container YANG statement.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 13, 2019
 */
class DtoGenerator extends AbstractGenerator {
    private final DataSchemaNode node;

    DtoGenerator(File baseDir, Module module, VelocityContext baseContext, DataSchemaNode node) {
        super(baseDir, module, baseContext);
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public List<File> generate() throws IOException {
        final List<File> result = new ArrayList<>();
        final String name = node.getQName().getLocalName();
        String currentPackage = (String) baseContext.get(PROP_JAVA_PACKAGE) + "." + yang2fieldName(name);
        final VelocityContext context = new VelocityContext(baseContext);
        context.put(PROP_JAVA_PACKAGE, currentPackage);
        context.put(PROP_JAVA_NAME, yang2javaClassName(name));
        context.put(PROP_YANG_MOD, module.getQNameModule());
        if (node instanceof DataNodeContainer) {
            final List<FieldInfo> fields = collectFields((DataNodeContainer) node, currentPackage, child -> {
                try {
                    result.addAll(new DtoGenerator(baseDir, module, context, child).generate());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            context.put(Util.FIELDS, fields);
        }
        final File file = getTargetFile(baseDir, currentPackage, yang2javaClassName(name));
        result.add(file);
        writeTemplate(context, file, Util.DTO);
        return result;
    }
}
