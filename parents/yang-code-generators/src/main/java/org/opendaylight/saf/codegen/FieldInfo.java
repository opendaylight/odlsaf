/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.yangtools.yang.model.api.AnyxmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

@SuppressFBWarnings({ "NM_CONFUSING" })
public class FieldInfo {
    private final String name;
    private final String javaname;
    private final String javatype;
    private final String description;

    public FieldInfo(LeafSchemaNode leafSchemaNode) {
        this.name = leafSchemaNode.getQName().getLocalName();
        this.javaname = Util.yang2fieldName(name);
        this.javatype = Util.toJavaType(leafSchemaNode.getType());
        this.description = Util.formatDescription(leafSchemaNode.getDescription());
    }

    public FieldInfo(String basePackage, ContainerSchemaNode child) {
        this.name = child.getQName().getLocalName();
        this.javaname = Util.yang2fieldName(name);
        this.javatype = basePackage + "." + Util.yang2fieldName(name) + "." + Util.yang2javaClassName(name);
        this.description = Util.formatDescription(child.getDescription());
    }

    public FieldInfo(AnyxmlSchemaNode child) {
        this.name = child.getQName().getLocalName();
        this.javaname = Util.yang2fieldName(name);
        this.javatype = "com.google.gson.JsonElement";
        this.description = Util.formatDescription(child.getDescription());
    }

    public FieldInfo(LeafListSchemaNode child) {
        this.name = child.getQName().getLocalName();
        this.javaname = Util.yang2fieldName(name);
        this.javatype = "java.util.List<" + Util.toJavaType(child.getType()) + ">";
        this.description = Util.formatDescription(child.getDescription());
    }

    public FieldInfo(String basePackage, ListSchemaNode child) {
        this.name = child.getQName().getLocalName();
        this.javaname = Util.yang2fieldName(name);
        final String innerType = basePackage + "." + Util.yang2fieldName(name) + "." + Util.yang2javaClassName(name);
        this.javatype = "java.util.List<" + innerType + ">";
        this.description = Util.formatDescription(child.getDescription());
    }

    public String getName() {
        return name;
    }

    public String getJavaname() {
        return javaname;
    }

    public String getJavatype() {
        return javatype;
    }

    public String getDescription() {
        return description;
    }
}
