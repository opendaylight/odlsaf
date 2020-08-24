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
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.opendaylight.saf.wfe.check.PathValidationCheck;
import org.opendaylight.saf.wfe.impl.model.CheckRequest;
import org.springframework.stereotype.Component;

/**
 * {@link JavaDelegate} to perform PRECHECK.
 *
 * @author Deepthi
 *
 */
@Component
public class PrecheckDelegate extends AbstractCheckDelegate {
    @PostConstruct
    public void init() {
        checks.put("pathValidation", new PathValidationCheck(client));
    }

    @Override
    protected List<String> getChecks(CheckRequest request) {
        return request.getPrechecks();
    }
}