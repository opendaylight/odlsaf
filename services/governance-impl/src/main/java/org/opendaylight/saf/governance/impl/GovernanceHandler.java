/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.impl;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.GovernanceInput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.GovernanceOutput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SafGovernanceRpcService;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SetGovernanceInput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SetGovernanceOutput;
import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.UnsetGovernanceInput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.FindInput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.FindOutput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.ListOutput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.PublishInput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.PublishOutput;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.SafYangLibraryRpcService;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.DependsInput;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.DependsOutput;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.SafYangModuleRpcService;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.SourceInput;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.SourceOutput;
import org.opendaylight.saf.springboot.annotation.Responder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Responder("${endpoint}")
public class GovernanceHandler
        implements SafGovernanceRpcService, SafYangLibraryRpcService, SafYangModuleRpcService {
    @Autowired
    private YangLibrary yangLibrary;

    @Autowired
    private Gson gson;

    @Autowired
    private Governance governance;

    @Override
    public GovernanceOutput governance(GovernanceInput input) {
        return GovernanceOutput.builder()
                .uri(governance.get(input.getEntity(), input.getPath(), input.getStore()))
                .build();
    }

    @Override
    public SetGovernanceOutput setGovernance(SetGovernanceInput input) {
        governance.set(input.getEntity(), input.getPath(), input.getStore(), input.getUri());
        return SetGovernanceOutput.builder().success(true).build();
    }

    @Override
    public void unsetGovernance(UnsetGovernanceInput input) {
        governance.delete(input.getEntity(), input.getPath(), input.getStore());
    }

    @Override
    public DependsOutput depends(DependsInput input) {
        return DependsOutput.builder()
                .modules(gson.toJsonTree(yangLibrary.dependsRecursive(input.getModule(), input.getRevision())))
                .build();
    }

    @Override
    public FindOutput find(FindInput input) {
        return FindOutput.builder()
                .modules(gson.toJsonTree(yangLibrary.findModules(input.getModule(), input.getRevision())))
                .build();
    }

    @Override
    public ListOutput list() {
        return ListOutput.builder().modules(gson.toJsonTree(yangLibrary.listAllYangModules())).build();
    }

    @Override
    public SourceOutput source(SourceInput input) {
        return SourceOutput.builder()
                .source(yangLibrary.getSource(input.getModule(), input.getRevision()).orElse(null))
                .build();
    }

    @Override
    public PublishOutput publish(PublishInput input) {
        Preconditions.checkArgument(input.getModules() != null, "List of modules is required, but was not provided");
        final PublishResult result = yangLibrary.publishModules(input.getModules());
        return PublishOutput.builder().reason(result.getResult()).success(result.isSuccess()).build();
    }

    @Override
    public void close() {
        // NOOP, nothing to close
    }
}
