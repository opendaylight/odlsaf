/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl;

import org.camunda.bpm.engine.impl.db.DbIdGenerator;
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaHistoryLevelAutoHandlingConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Use {@link StrongUuidGenerator} to avoid potential problems in cluster environments with {@link DbIdGenerator}.
 *
 * @author spichandi
 */
@Configuration
public class CamundaIdGeneratorConfiguration extends AbstractCamundaConfiguration
        implements CamundaHistoryLevelAutoHandlingConfiguration {

    @Override
    public void preInit(SpringProcessEngineConfiguration configuration) {
        configuration.setIdGenerator(new StrongUuidGenerator());
    }
}