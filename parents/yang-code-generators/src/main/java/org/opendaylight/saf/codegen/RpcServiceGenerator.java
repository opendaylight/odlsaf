/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import java.io.File;
import java.util.Collection;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Generator that emits RPC service class.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 11, 2019
 */
class RpcServiceGenerator extends AbstractServiceGenerator<RpcDefinition> {

    RpcServiceGenerator(File baseDir, Module module, VelocityContext context) {
        super(baseDir, module, context);
    }

    @Override
    protected Collection<? extends RpcDefinition> getChildNodes() {
        return module.getRpcs();
    }

    @Override
    protected String getServiceFileName() {
        return Util.getRpcServiceName(module);
    }
}
