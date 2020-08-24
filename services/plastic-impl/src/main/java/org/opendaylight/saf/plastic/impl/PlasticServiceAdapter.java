/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.plastic.impl;

import static org.opendaylight.plastic.implementation.Cartography.EMPTY_DEFAULTS;

import java.util.Optional;
import org.opendaylight.plastic.implementation.CartographerWorker;
import org.opendaylight.plastic.implementation.SearchPath;
import org.opendaylight.plastic.implementation.VersionedSchema;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.SafPlasticRpcService;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.TranslateInput;
import org.opendaylight.saf.plastic.api.saf_plastic.gen.rev20180411.TranslateOutput;
import org.opendaylight.saf.springboot.annotation.Responder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Responder("${endpoint}")
public class PlasticServiceAdapter implements SafPlasticRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(PlasticServiceAdapter.class);
    private static final String NOTPROPERTYFILENAME = "%s;";
    private final CartographerWorker worker;

    public PlasticServiceAdapter(@Value("${plasticRoot}") String rootDirectory,
            @Value("${poll.interval:5}") int pollIntervalSec) {
        LOG.info("Root directory : {}", rootDirectory);
        LOG.info("FS poll interval : {} seconds", pollIntervalSec);
        this.worker = new CartographerWorker(new SearchPath(String.format(NOTPROPERTYFILENAME, rootDirectory)),
                pollIntervalSec);
    }

    @Override
    public TranslateOutput translate(TranslateInput input) {
        final VersionedSchema in = new VersionedSchema(input.getInName(), input.getInVersion(), input.getInType());
        final VersionedSchema out = new VersionedSchema(input.getOutName(), input.getOutVersion(), input.getOutType());
        return TranslateOutput.builder()
                .data(worker.translateWithDefaults(in, out, input.getData(),
                        Optional.ofNullable(input.getDefaults()).orElse(EMPTY_DEFAULTS)))
                .build();
    }
}
