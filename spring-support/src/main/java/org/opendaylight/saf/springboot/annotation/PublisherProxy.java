/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Member field inside managed bean annotated with this annotation will be set to proxy exposed as publisher.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jul 28, 2019
 */
@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface PublisherProxy {
    /**
     * Endpoint address to connect to. It can be either in form of placeholder like <code>"${my.endpoint}"</code> or
     * URI <code>"zmq://127.0.0.1:12000"</code>.
     */
    String value();
}
