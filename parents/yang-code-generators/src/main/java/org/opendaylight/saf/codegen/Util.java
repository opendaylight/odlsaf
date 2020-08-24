/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.codegen;

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.opendaylight.mdsal.binding.spec.naming.BindingMapping;
import org.opendaylight.yangtools.yang.model.api.AnyxmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Int16TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Int32TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Int64TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Int8TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint16TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint32TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint64TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint8TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    private static final VelocityEngine ENGINE;
    public static final String DTO = "dto";
    public static final String FIELDS = "fields";
    public static final String PROP_JAVA_PACKAGE = "javapackage";
    public static final String PROP_JAVA_NAME = "javaname";
    public static final String PROP_YANG_MOD = "yangmodule";
    public static final String CFG_JAVA_PACKAGE_PREFIX = "java.package.prefix";

    static {
        ENGINE = new VelocityEngine();
        ENGINE.init();
    }

    private Util() {
        // utility class constructor
    }

    public static InputStream getTemplate(String name) throws IOException {
        return Resources.getResource(Util.class, "/" + name + ".vm").openStream();
    }

    public static String renderTemplate(String name, VelocityContext context) throws IOException {
        final StringWriter sw = new StringWriter();
        try (InputStream is = getTemplate(name);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Preconditions.checkState(ENGINE.evaluate(context, sw, name, reader), "Template render failed");
            return sw.toString();
        }
    }

    /**
     * Map module name to class name.
     *
     * <p>
     * Example:
     *
     * <pre>
     * ldk-yang-library =&gt; LdkYangLibrary
     * </pre>
     *
     * @param module module to get class name from
     * @return class name
     */
    public static String getClassName(Module module) {
        return yang2javaClassName(module.getName());
    }

    public static String yang2javaClassName(String name) {
        return Arrays.asList(name.replaceAll("_", "-").split("-"))
                .stream()
                .map(part -> part.substring(0, 1).toUpperCase(Locale.US) + part.substring(1).toLowerCase(Locale.US))
                .collect(Collectors.joining(""));
    }

    public static String yang2fieldName(String name) {
        String fieldName = yang2javaClassName(name);
        fieldName = fieldName.substring(0, 1).toLowerCase(Locale.US) + fieldName.substring(1);
        if (BindingMapping.JAVA_RESERVED_WORDS.contains(fieldName)) {
            return "_" + fieldName;
        } else {
            return fieldName;
        }
    }

    public static String getJavaPackage(String prefix, Module module) {
        final String packageName = new StringBuilder().append(prefix.replace("-", "_"))
                .append(".")
                .append(module.getName().replaceAll("-", "_"))
                .append(".gen.rev")
                .append(module.getRevision().get().toString().replaceAll("-", ""))
                .toString();
        LOG.debug("Mapped package '{}' for module {}", packageName, module);
        return packageName;
    }

    public static Predicate<DataSchemaNode> possibleNodesFilter() {
        return node -> node instanceof LeafSchemaNode || node instanceof AnyxmlSchemaNode
                || node instanceof ContainerSchemaNode;
    }

    public static File getTargetFile(File basedir, String currentPackage, String name) {
        LOG.debug("Current package : {}", currentPackage);
        final Path dir = Paths.get(basedir.getAbsolutePath()).resolve(currentPackage.replace(".", "/"));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return dir.resolve(name + ".java").toFile();
    }

    public static String toJavaType(TypeDefinition<? extends TypeDefinition<?>> type) {
        if (type.getBaseType() == null) {
            if (type instanceof BooleanTypeDefinition) {
                return "java.lang.Boolean";
            }
            if (type instanceof StringTypeDefinition || type instanceof UnionTypeDefinition
                    || type instanceof EnumTypeDefinition || type instanceof LeafrefTypeDefinition
                    || type instanceof IdentityrefTypeDefinition || type instanceof InstanceIdentifierTypeDefinition) {
                return "java.lang.String";
            }
            if (type instanceof Uint64TypeDefinition || type instanceof Int64TypeDefinition
                    || type instanceof Uint32TypeDefinition || type instanceof Int32TypeDefinition) {
                return "java.lang.Long";
            }
            if (type instanceof Uint16TypeDefinition || type instanceof Int16TypeDefinition
                    || type instanceof Uint8TypeDefinition || type instanceof Int8TypeDefinition) {
                return "java.lang.Integer";
            }
            if (type instanceof BinaryTypeDefinition) {
                return "byte[]";
            }
            throw new UnsupportedOperationException("Unsupported type " + type);
        } else {
            return toJavaType(type.getBaseType());
        }
    }

    public static String toPythonType(TypeDefinition<? extends TypeDefinition<?>> type) {
        if (type.getBaseType() == null) {
            if (type instanceof BooleanTypeDefinition) {
                return "BUILTIN_TYPES['boolean']";
            }
            if (type instanceof StringTypeDefinition || type instanceof UnionTypeDefinition
                    || type instanceof EnumTypeDefinition || type instanceof LeafrefTypeDefinition
                    || type instanceof IdentityrefTypeDefinition || type instanceof InstanceIdentifierTypeDefinition) {
                return "BUILTIN_TYPES['string']";
            }
            if (type instanceof Uint64TypeDefinition || type instanceof Int64TypeDefinition
                    || type instanceof Uint32TypeDefinition || type instanceof Int32TypeDefinition) {
                return "BUILTIN_TYPES['int64']";
            }
            if (type instanceof Uint16TypeDefinition || type instanceof Int16TypeDefinition
                    || type instanceof Uint8TypeDefinition || type instanceof Int8TypeDefinition) {
                return "BUILTIN_TYPES['int32']";
            }
            if (type instanceof BinaryTypeDefinition) {
                return "BUILTIN_TYPES['binary']";
            }
            throw new UnsupportedOperationException("Unsupported type " + type);
        } else {
            return toPythonType(type.getBaseType());
        }
    }

    public static String getRpcServiceName(Module module) {
        return getClassName(module) + "RpcService";
    }

    public static String getNotificationServiceName(Module module) {
        return getClassName(module) + "NotificationService";
    }

    public static String formatDescription(Optional<String> input) {
        return input.isPresent() ? "/**\n" + Arrays.asList(input.get().split("\\r?\\n"))
                .stream()
                .map(line -> ("     * " + line))
                .collect(Collectors.joining("\n")) + "\n     */\n" : null;
    }

    public static String formatPythonDescription(Optional<String> input) {
        return input.isPresent() ? "'''\n" + Arrays.asList(input.get().split("\\r?\\n"))
                .stream()
                .map(line -> ("           " + line))
                .collect(Collectors.joining("\n")) + "\n         '''\n" : null;
    }

    private static final Predicate<DataSchemaNode> NODE_FILTER = dsn -> {
        if (dsn instanceof LeafSchemaNode && ((LeafSchemaNode) dsn).getType() instanceof EmptyTypeDefinition) {
            return false;
        }
        return true;
    };

    public static Collection<DataSchemaNode> filterNodes(Collection<? extends DataSchemaNode> unfiltered) {
        return unfiltered.stream().filter(NODE_FILTER).collect(Collectors.toList());
    }

    public static List<FieldInfo> collectFields(DataNodeContainer node, final String basePackage) {
        return collectFields(node, basePackage, callback -> {
        });
    }

    public static List<FieldInfo> collectFields(DataNodeContainer node, final String basePackage,
            Consumer<DataSchemaNode> childCallback) {
        final List<FieldInfo> fields = new ArrayList<>();
        for (DataSchemaNode child : filterNodes(node.getChildNodes())) {
            LOG.debug("Child node : {}", child);
            if (child instanceof ContainerSchemaNode) {
                fields.add(new FieldInfo(basePackage, (ContainerSchemaNode) child));
                childCallback.accept(child);
            }
            if (child instanceof ListSchemaNode) {
                fields.add(new FieldInfo(basePackage, (ListSchemaNode) child));
                childCallback.accept(child);
            }
            if (child instanceof LeafSchemaNode) {
                fields.add(new FieldInfo((LeafSchemaNode) child));
            }
            if (child instanceof LeafListSchemaNode) {
                fields.add(new FieldInfo((LeafListSchemaNode) child));
            }
            if (child instanceof AnyxmlSchemaNode) {
                fields.add(new FieldInfo((AnyxmlSchemaNode) child));
            }
        }
        return fields;
    }
}
