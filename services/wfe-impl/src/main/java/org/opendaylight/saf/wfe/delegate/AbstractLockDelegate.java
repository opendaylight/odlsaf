/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.wfe.impl.model.LockService;
import org.opendaylight.saf.wfe.impl.model.YangPathArgument;
import org.opendaylight.saf.wfe.util.DelegateConstants;
import org.opendaylight.saf.wfe.util.DelegateUtils;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractLockDelegate extends AbstractDelegate implements JavaDelegate {
    private static final TypeToken<List<YangPathArgument>> TT = new TypeToken<>() {
    };

    @Autowired
    protected LockService lockService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        getInput(execution, TT).stream()
                .map(YangPathArgument::getYangPath)
                .filter(yangPath -> yangPath.startsWith(DelegateConstants.JSONRPC_BASE_URI))
                .map(DelegateUtils::extractDeviceName)
                .forEach(device -> performAction(device, execution.getProcessInstanceId()));
    }

    protected abstract void performAction(String device, String id);
}
