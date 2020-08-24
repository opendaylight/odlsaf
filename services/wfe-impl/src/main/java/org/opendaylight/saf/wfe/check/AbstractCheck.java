/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.check;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;
import org.opendaylight.saf.wfe.impl.model.CheckArg;
import org.opendaylight.saf.wfe.impl.model.HttpResponse;
import org.opendaylight.saf.wfe.impl.model.RestconfClient;
import org.opendaylight.saf.wfe.util.DelegateConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractCheck implements Predicate<CheckArg> {
    @SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractCheck.class);
    private final RestconfClient client;

    AbstractCheck(final RestconfClient lscClient) {
        this.client = Objects.requireNonNull(lscClient);
    }

    protected HttpResponse callRestconf(String path) {
        try {
            return client.call(DelegateConstants.GET_REQUEST, DelegateConstants.RESTCONF_BASE + path, null);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to verify change", e);
        }
    }
}
