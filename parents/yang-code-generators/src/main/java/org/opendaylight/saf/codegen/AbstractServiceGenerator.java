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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

abstract class AbstractServiceGenerator<T extends SchemaNode> extends AbstractGenerator {

    AbstractServiceGenerator(File baseDir, Module module, VelocityContext baseContext) {
        super(baseDir, module, baseContext);
    }

    protected abstract Collection<? extends T> getChildNodes();

    protected abstract String getServiceFileName();

    @Override
    public List<File> generate() throws IOException {
        final String basePackage = (String) baseContext.get(PROP_JAVA_PACKAGE);
        final List<File> result = new ArrayList<>();
        final List<MethodInfo> methods = new ArrayList<>();
        final VelocityContext context = new VelocityContext(baseContext);
        context.put(PROP_JAVA_NAME, getServiceFileName());
        context.put(PROP_JAVA_PACKAGE, baseContext.get(PROP_JAVA_PACKAGE));
        context.put(PROP_YANG_MOD, module.getQNameModule());
        for (T not : getChildNodes()) {
            final MethodInfo mi = new MethodInfo(not, basePackage);
            LOG.debug("Found node definition {}", mi);
            methods.add(mi);

        }
        final File out = Util.getTargetFile(baseDir, basePackage, getServiceFileName());
        context.put("methods", methods);
        writeTemplate(context, out, "api");

        result.add(out);
        return result;
    }
}
