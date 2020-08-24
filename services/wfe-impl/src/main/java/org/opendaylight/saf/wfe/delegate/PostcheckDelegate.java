/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.delegate;

import java.util.List;
import javax.annotation.PostConstruct;
import org.opendaylight.saf.wfe.check.VerifyDelete;
import org.opendaylight.saf.wfe.check.VerifyPutCheck;
import org.opendaylight.saf.wfe.impl.model.CheckRequest;
import org.springframework.stereotype.Component;

@Component
public class PostcheckDelegate extends AbstractCheckDelegate {
    @PostConstruct
    public void init() {
        checks.put("VerifyPut", new VerifyPutCheck(client));
        checks.put("VerifyDelete", new VerifyDelete(client));
    }

    @Override
    protected List<String> getChecks(CheckRequest request) {
        return request.getPostchecks();
    }
}
