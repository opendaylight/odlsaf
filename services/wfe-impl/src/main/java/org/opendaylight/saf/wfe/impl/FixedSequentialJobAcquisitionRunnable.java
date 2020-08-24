/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.saf.wfe.impl;

import org.camunda.bpm.engine.impl.jobexecutor.JobAcquisitionContext;
import org.camunda.bpm.engine.impl.jobexecutor.JobAcquisitionStrategy;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.camunda.bpm.engine.impl.jobexecutor.SequentialJobAcquisitionRunnable;

public class FixedSequentialJobAcquisitionRunnable extends SequentialJobAcquisitionRunnable {

    public FixedSequentialJobAcquisitionRunnable(JobExecutor jobExecutor) {
        super(jobExecutor);
    }

    @Override
    protected void configureNextAcquisitionCycle(JobAcquisitionContext acquisitionContext,
            JobAcquisitionStrategy acquisitionStrategy) {
        super.configureNextAcquisitionCycle(acquisitionContext, acquisitionStrategy);
        isJobAdded = false;
    }
}
