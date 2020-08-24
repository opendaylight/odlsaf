/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import org.opendaylight.saf.wfe.deployer.ChangeDeployer;

/**
 * Action to perform in {@link ChangeDeployer}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 30, 2019
 */
public enum ActionType {
    /**
     * Process definition was created.
     */
    CREATE,

    /**
     * Process definition was deleted.
     */
    DELETE;
}