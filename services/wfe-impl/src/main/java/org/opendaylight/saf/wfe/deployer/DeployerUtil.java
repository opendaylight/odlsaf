/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.deployer;

import java.nio.file.Path;

public final class DeployerUtil {
    private DeployerUtil() {
        // utility class constructor
    }

    public static boolean isBpmnFile(Path candidate) {
        return candidate.toString().endsWith(".bpmn");
    }
}
