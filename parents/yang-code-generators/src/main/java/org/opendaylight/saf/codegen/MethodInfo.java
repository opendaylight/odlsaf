/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import static org.opendaylight.saf.codegen.Util.collectFields;
import static org.opendaylight.saf.codegen.Util.formatDescription;
import static org.opendaylight.saf.codegen.Util.yang2fieldName;
import static org.opendaylight.saf.codegen.Util.yang2javaClassName;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yangtools.yang.model.api.DocumentedNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

@Data
@Builder
@AllArgsConstructor
@SuppressFBWarnings("URF_UNREAD_FIELD")
public class MethodInfo {
    private final String name;
    private final String inputJavaType;
    private final String javaName;
    private final String outputJavaType;
    private final String paramname;
    private final String description;
    private final List<FieldInfo> inputFields;
    private final String basePackage;

    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public MethodInfo(DocumentedNode def, String basePackage) {
        this.basePackage = basePackage;
        if (def instanceof RpcDefinition) {
            final RpcDefinition rpcDef = (RpcDefinition) def;
            this.name = rpcDef.getQName().getLocalName();
            this.javaName = yang2fieldName(name);
            this.description = formatDescription(rpcDef.getDescription());
            if (rpcDef.getInput().getChildNodes().isEmpty()) {
                this.inputJavaType = "";
                this.paramname = "";
                this.inputFields = Collections.emptyList();
            } else {
                this.inputJavaType = yang2javaClassName(name) + "Input";
                this.inputFields = collectFields(rpcDef.getInput(), basePackage);
                this.paramname = "input";
            }
            if (rpcDef.getOutput().getChildNodes().isEmpty()) {
                this.outputJavaType = "void";
            } else {
                this.outputJavaType = yang2javaClassName(name) + "Output";
            }
        } else if (def instanceof NotificationDefinition) {
            final NotificationDefinition notDef = (NotificationDefinition) def;
            this.description = formatDescription(notDef.getDescription());
            this.name = notDef.getQName().getLocalName();
            this.javaName = yang2fieldName(name);
            this.inputJavaType = yang2javaClassName(name);
            this.outputJavaType = "void";
            this.paramname = "notification";
            this.inputFields = collectFields(notDef, basePackage);
        } else {
            throw new IllegalArgumentException("Unsupported schema node : " + def);
        }
    }
}
