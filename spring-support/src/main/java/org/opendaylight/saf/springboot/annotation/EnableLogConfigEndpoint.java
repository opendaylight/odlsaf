/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.opendaylight.saf.springboot.internal.LogConfigEndpointConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * This annotation can by used on application entry class (that is class annotated with {@link SpringBootApplication})
 * to enable JSONRPC responder for logging configuration.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Oct 22, 2019
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(LogConfigEndpointConfiguration.class)
public @interface EnableLogConfigEndpoint {
    /**
     * Endpoint to bind responder to.
     *
     * @return URI of responder
     */
    String value() default "ws://127.0.0.1:8001";
}
