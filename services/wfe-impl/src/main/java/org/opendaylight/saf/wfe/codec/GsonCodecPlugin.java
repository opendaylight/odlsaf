/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.codec;

import com.google.common.net.MediaType;
import java.util.Collections;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Plugin that register custom serializer and forces serialization format to be application/json.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jun 13, 2019
 */
@Configuration
public class GsonCodecPlugin extends AbstractCamundaConfiguration {
    @Autowired
    private JsonObjectSerializer codec;

    @Override
    public void preInit(ProcessEngineConfigurationImpl config) {
        forceConfig(config);
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl config) {
        forceConfig(config);
    }

    private void forceConfig(ProcessEngineConfigurationImpl config) {
        config.setCustomPreVariableSerializers(Collections.singletonList(codec));
        config.setDefaultSerializationFormat(MediaType.JSON_UTF_8.toString());
    }
}
