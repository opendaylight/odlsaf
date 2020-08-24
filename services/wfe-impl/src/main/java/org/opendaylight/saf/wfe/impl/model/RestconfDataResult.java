/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.impl.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple wrapper to avoid problems when returned data are being serialized as anyxml.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jun 11, 2019
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/*
 * Subclasses of JsonElement does not implement java.io.Serializable interface. So we need to use explicit
 * serializer in camunda. Already requested in past, but GSON folks doesn't like that idea :
 * https://github.com/google/gson/issues/485
 */
@SuppressWarnings("squid:S1948")
@SuppressFBWarnings("SE_BAD_FIELD")
public class RestconfDataResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private JsonElement content;
    @SerializedName("yang-path")
    private String yangPath;
    private String error;
    private String entity;
}
