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
import java.util.Objects;
import org.apache.velocity.VelocityContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;

class NotificationPayloadGenerator extends DataNodeContainerChildGenerator {
    private final NotificationDefinition notification;

    NotificationPayloadGenerator(File resourceBaseDir, Module module, VelocityContext vc,
            NotificationDefinition notification) {
        super(resourceBaseDir, module, vc);
        this.notification = Objects.requireNonNull(notification);
    }

    @Override
    public List<File> generate() throws IOException {
        final List<File> result = new ArrayList<>();
        final String basePackage = (String) baseContext.get(PROP_JAVA_PACKAGE);
        final VelocityContext context = new VelocityContext(baseContext);
        context.put(PROP_JAVA_PACKAGE, basePackage);

        process(result, basePackage, context, notification.getQName().getLocalName(), "", notification);

        return result;
    }
}
