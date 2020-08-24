/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class HttpDataRequest {
    @SerializedName("method")
    private String method;
    @SerializedName("uri")
    private String uri;
    @SerializedName("data")
    private JsonObject data;
    @SerializedName("headers")
    private JsonObject headers;
    @SerializedName("connection-timeout")
    private Integer connectionTimeout;
    @SerializedName("read-timeout")
    private Integer readTimeout;
    @SerializedName("response-variable-name")
    private String responseVariableName;

}