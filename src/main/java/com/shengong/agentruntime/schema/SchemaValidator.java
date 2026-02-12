package com.shengong.agentruntime.schema;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Basic schema validation utility for map payloads.
 */
@Component
public class SchemaValidator {

    public List<String> validate(Schema schema, Map<String, Object> payload) {
        if (schema == null || schema.isEmpty()) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();

        if (schema.getRequired() != null) {
            for (String required : schema.getRequired()) {
                Object value = payload != null ? payload.get(required) : null;
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    errors.add("Missing required field: " + required);
                }
            }
        }

        if (schema.getProperties() != null && payload != null) {
            for (Map.Entry<String, SchemaProperty> entry : schema.getProperties().entrySet()) {
                String field = entry.getKey();
                SchemaProperty property = entry.getValue();
                if (property == null) {
                    continue;
                }
                Object value = payload.get(field);
                if (value == null) {
                    continue;
                }
                if (!typeMatches(property.getType(), value)) {
                    errors.add("Field '" + field + "' type mismatch: expected "
                        + property.getType() + ", got " + value.getClass().getSimpleName());
                }
            }
        }

        return errors;
    }

    private boolean typeMatches(String type, Object value) {
        if (type == null || type.isEmpty()) {
            return true;
        }
        String normalized = type.toLowerCase();
        return switch (normalized) {
            case "string" -> value instanceof String;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "object" -> value instanceof Map;
            case "array" -> value instanceof List;
            default -> true;
        };
    }
}
