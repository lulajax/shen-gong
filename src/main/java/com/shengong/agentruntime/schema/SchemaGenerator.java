package com.shengong.agentruntime.schema;

import com.shengong.agentruntime.core.param.AgentParam;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates lightweight schema from annotated DTOs.
 */
public class SchemaGenerator {

    public Schema generate(String id, String title, Class<?> paramType) {
        Schema schema = Schema.object(id, title);
        if (paramType == null) {
            return schema;
        }

        Map<String, SchemaProperty> properties = new LinkedHashMap<>();
        for (Field field : paramType.getDeclaredFields()) {
            AgentParam meta = field.getAnnotation(AgentParam.class);
            if (meta == null) {
                continue;
            }
            String type = mapType(field.getType());
            SchemaProperty property = SchemaProperty.of(type, meta.description(), meta.example());
            properties.put(field.getName(), property);
            if (meta.required()) {
                schema.getRequired().add(field.getName());
            }
        }
        schema.setProperties(properties);
        return schema;
    }

    private String mapType(Class<?> type) {
        if (type == null) {
            return "object";
        }
        if (String.class.equals(type)) {
            return "string";
        }
        if (Number.class.isAssignableFrom(type)
            || int.class.equals(type)
            || long.class.equals(type)
            || double.class.equals(type)
            || float.class.equals(type)
            || short.class.equals(type)) {
            return "number";
        }
        if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            return "boolean";
        }
        if (type.isArray() || java.util.List.class.isAssignableFrom(type)) {
            return "array";
        }
        if (java.util.Map.class.isAssignableFrom(type)) {
            return "object";
        }
        return "object";
    }
}
