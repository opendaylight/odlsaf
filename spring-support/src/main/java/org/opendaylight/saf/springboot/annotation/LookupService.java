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
 * This annotation can be used to indicate that actual endpoint is not known as should be obtained from service
 * registry.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since May 17, 2020
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface LookupService {
    /**
     * Name of entity to lookup in service registry.
     *
     * @return name of entity to lookup in service registry
     */
    String entity();

    /**
     * Name of store, either "config", "oerational" or "none".
     *
     * @return name of store
     */
    String store() default "none";

    /**
     * Path within tree.
     *
     * @return path
     */
    String path() default "{}";
}
