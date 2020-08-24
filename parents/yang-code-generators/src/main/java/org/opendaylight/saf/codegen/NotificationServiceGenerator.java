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
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;

/**
 * This generator emits notification service API.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 15, 2019
 */
class NotificationServiceGenerator extends AbstractServiceGenerator<NotificationDefinition> {

    NotificationServiceGenerator(File resourceBaseDir, Module module, VelocityContext vc) {
        super(resourceBaseDir, module, vc);
    }

    @Override
    protected Collection<? extends NotificationDefinition> getChildNodes() {
        return module.getNotifications();
    }

    @Override
    protected String getServiceFileName() {
        return Util.getNotificationServiceName(module);
    }
}
