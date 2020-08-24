/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.apache.ibatis.io.Resources;
import org.junit.Test;
import org.opendaylight.saf.wfe.impl.model.DeploymentItem;

public class DeploymentItemTest {
    @Test
    public void testParseBpmn() throws IOException {
        Set<Path> scripts;
        scripts = DeploymentItem.parseBpmnResources(Resources.getResourceAsStream("test1.bpmn"),
                DeploymentItem.DEP_PREFIX);
        assertEquals(3, scripts.size());
        scripts = DeploymentItem.parseBpmnResources(Resources.getResourceAsStream("test2.bpmn"),
                DeploymentItem.DEP_PREFIX);
        assertEquals(0, scripts.size());
    }
}
