/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.governance.client;

import org.opendaylight.saf.governance.api.saf_governance.gen.rev20200227.SafGovernanceRpcService;
import org.opendaylight.saf.governance.api.saf_yang_library.gen.rev20200227.SafYangLibraryRpcService;
import org.opendaylight.saf.governance.api.saf_yang_module.gen.rev20200227.SafYangModuleRpcService;

public interface GovernanceCompositeInterface
        extends SafYangLibraryRpcService, SafYangModuleRpcService, SafGovernanceRpcService {

    @Override
    default void close() {
        SafYangModuleRpcService.super.close();
        SafYangLibraryRpcService.super.close();
        SafGovernanceRpcService.super.close();
    }
}
