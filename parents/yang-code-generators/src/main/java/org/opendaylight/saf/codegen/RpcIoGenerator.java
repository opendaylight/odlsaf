/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import static org.opendaylight.saf.codegen.Util.PROP_JAVA_PACKAGE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Generator that emits RPC input/output object class and all referenced nodes.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 11, 2019
 */
class RpcIoGenerator extends DataNodeContainerChildGenerator {
    private final RpcDefinition rpc;

    RpcIoGenerator(File baseDir, Module module, VelocityContext context, RpcDefinition rpc) {
        super(baseDir, module, context);
        this.rpc = rpc;
    }

    @Override
    public List<File> generate() throws IOException {
        final List<File> result = new ArrayList<>();
        final String basePackage = (String) baseContext.get(PROP_JAVA_PACKAGE);
        final VelocityContext context = new VelocityContext(baseContext);
        context.put(PROP_JAVA_PACKAGE, basePackage);
        if (!rpc.getInput().getChildNodes().isEmpty()) {
            process(result, basePackage, context, rpc.getQName().getLocalName(), "Input", rpc.getInput());
        }
        if (!rpc.getOutput().getChildNodes().isEmpty()) {
            process(result, basePackage, context, rpc.getQName().getLocalName(), "Output", rpc.getOutput());
        }
        return result;
    }
}
