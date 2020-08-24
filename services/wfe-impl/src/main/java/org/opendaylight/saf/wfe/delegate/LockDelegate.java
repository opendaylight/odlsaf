/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import com.google.common.base.Verify;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link JavaDelegate} to LOCK device in device database.
 *
 * @author spichandi
 *
 */
@ConditionalOnProperty(name = "devicedb")
@Component
public class LockDelegate extends AbstractLockDelegate {
    @Override
    protected void performAction(String device, String id) {
        Verify.verify(lockService.lock(device, id), "Unable to lock device '%s'", device);
    }
}