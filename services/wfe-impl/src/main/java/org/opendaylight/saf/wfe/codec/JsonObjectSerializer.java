/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.saf.wfe.codec;

import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.camunda.bpm.engine.impl.variable.serializer.AbstractTypedValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Custom serializer for application/json format.
 *
 * @author <a href="mailto:rkosegi@luminanetworks.com">Richard Kosegi</a>
 * @since Jun 13, 2019
 */
@Component
public class JsonObjectSerializer extends AbstractTypedValueSerializer<ObjectValue> {
    private static final JsonParser PARSER = new JsonParser();
    @Autowired
    private Gson gson;

    public JsonObjectSerializer() {
        super(ValueType.OBJECT);
    }

    @Override
    public String getName() {
        return "gson";
    }

    @Override
    public String getSerializationDataformat() {
        return MediaType.JSON_UTF_8.toString();
    }

    @Override
    public void writeValue(ObjectValue objectValue, ValueFields valueFields) {
        final Object value = objectValue.getValue();
        if (value != null) {
            valueFields.setByteArrayValue(gson.toJson(value).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public ObjectValue readValue(ValueFields valueFields, boolean deserializeValue) {
        if (valueFields.getByteArrayValue() != null) {
            Object data = PARSER.parse(new String(valueFields.getByteArrayValue(), StandardCharsets.UTF_8));
            return Variables.objectValue(data).create();
        }
        return Variables.objectValue(null).create();
    }

    @Override
    public ObjectValue convertToTypedValue(UntypedValueImpl untypedValue) {
        return Variables.objectValue(untypedValue.getValue()).create();
    }

    protected boolean isDeserializedObjectValue(TypedValue value) {
        return value instanceof ObjectValue && ((ObjectValue) value).isDeserialized();
    }

    @Override
    protected boolean canWriteValue(TypedValue value) {
        return isDeserializedObjectValue(value) || value instanceof UntypedValueImpl;
    }
}
