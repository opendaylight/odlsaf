/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Member field inside managed bean annotated with this annotation will be set to proxy connected to remote responder.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 28, 2019
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface RequesterProxy {
    /**
     * Endpoint address to connect to. It can be either in form of placeholder like <code>"${my.endpoint}"</code> or
     * URI <code>"zmq://127.0.0.1:12000"</code>. Value is required unless {@link LookupService} annotation is stacked
     * onto this one, in which case actual endpoint is obtained from governance.
     */
    String value() default "";

    /**
     * When proxy is marked as 'lazy', then actual endpoint is queried from governance whenever method is called on
     * that proxy - not at bean creation time.
     *
     * @return flag to indicate laziness of initialization.
     */
    boolean lazy() default false;
}
