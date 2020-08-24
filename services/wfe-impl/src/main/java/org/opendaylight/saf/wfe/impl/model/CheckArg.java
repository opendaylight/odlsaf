/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.saf.wfe.util.DelegateUtils;

@Data
@Builder
@AllArgsConstructor
public class CheckArg {
    private String yangPath;
    private JsonElement configData;
    private String check;

    public static CheckArg fromRequest(CheckRequest request, String checkName) {
        return builder().yangPath(DelegateUtils.getEncodedPath(request.getYangPath()))
                .check(checkName)
                .configData(request.getConfigData())
                .build();
    }
}
