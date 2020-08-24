/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.devicedb.impl;

import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.GovernanceInput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.GovernanceOutput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SafGovernanceRpcService;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SetGovernanceInput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SetGovernanceOutput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.UnsetGovernanceInput;
import org.opendaylight.saf.springboot.annotation.Responder;
import org.springframework.stereotype.Component;

@Component
@Responder("${governance}")
public class MockGovernanceImpl implements SafGovernanceRpcService {

    @Override
    public SetGovernanceOutput setGovernance(SetGovernanceInput input) {
        return SetGovernanceOutput.builder().success(true).build();
    }

    @Override
    public void unsetGovernance(UnsetGovernanceInput input) {

    }

    @Override
    public GovernanceOutput governance(GovernanceInput input) {
        return null;
    }
}
