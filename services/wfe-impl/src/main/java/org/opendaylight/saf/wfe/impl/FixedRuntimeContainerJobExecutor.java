/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.saf.wfe.impl;

import org.camunda.bpm.engine.impl.jobexecutor.DefaultAcquireJobsCommandFactory;
import org.camunda.bpm.engine.impl.jobexecutor.RuntimeContainerJobExecutor;

public class FixedRuntimeContainerJobExecutor extends RuntimeContainerJobExecutor {
    @Override
    protected void ensureInitialization() {
        acquireJobsCmdFactory = new DefaultAcquireJobsCommandFactory(this);
        acquireJobsRunnable = new FixedSequentialJobAcquisitionRunnable(this);
    }
}
