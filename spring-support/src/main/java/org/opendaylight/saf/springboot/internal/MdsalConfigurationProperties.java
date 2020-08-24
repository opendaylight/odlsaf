/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.internal;

import lombok.Data;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for {@link MdsalConfiguration}.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Feb 29, 2020
 */
@Data
@ConfigurationProperties(prefix = "mdsal")
public class MdsalConfigurationProperties {
    /**
     * Regular expression to allow filtering of YANG models that will be part of {@link EffectiveModelContext}. When
     * empty (or null), then all modules discovered on classpath (via {@link BindingReflections}) will be used. All
     * dependent modules are included automatically (and must be present on classpath). When specified, value
     * <strong>must be valid regular expression</strong> otherwise runtime exception might prevent application from
     * start.
     */
    private String schemaModules = "";
}
