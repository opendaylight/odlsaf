/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class YangPathArgument {
    @SerializedName("yang-path")
    private String yangPath;

}
