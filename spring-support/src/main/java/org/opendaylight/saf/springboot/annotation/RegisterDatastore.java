/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.springboot.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.opendaylight.saf.governance.api.saf_store_data.gen.rev20200227.SafStoreDataRpcService;

/**
 * Use this annotation to register {@link Responder} instance with governance if it implements
 * {@link SafStoreDataRpcService}.
 *
 * @author <a href="mailto:rkosegi@luminanetowrks.com">Richard Kosegi</a>
 * @since May 17, 2020
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface RegisterDatastore {
    /**
     * Name of entity to register in service registry.
     *
     * @return name of entity to register in service registry
     */
    String entity();

    /**
     * When true, datastore will handle configuration store requests.
     *
     * @return flag to indicate inclusion of config datastore
     */
    boolean config() default true;

    /**
     * When true, datastore will handle operational store requests.
     *
     * @return flag to indicate inclusion of operational datastore
     */
    boolean operational() default true;

    /**
     * Path within tree.
     *
     * @return path
     */
    String path() default "{}";

}
