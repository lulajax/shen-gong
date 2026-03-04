package com.shengong.agentruntime.service.runtime.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSchemaValidationServiceTest {

    private final JsonSchemaValidationService validationService =
            new JsonSchemaValidationService(new ObjectMapper());

    @Test
    void shouldFailWhenRequiredFieldMissing() {
        Map<String, Object> schema = Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "required", List.of("region"),
                "additionalProperties", false,
                "properties", Map.of(
                        "region", Map.of("type", "string")
                )
        );

        List<String> errors = validationService.validate(schema, Map.of());
        assertFalse(errors.isEmpty());
    }

    @Test
    void shouldFailWhenAdditionalPropertiesNotAllowed() {
        Map<String, Object> schema = Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "region", Map.of("type", "string")
                )
        );

        List<String> errors = validationService.validate(schema, Map.of("unexpected", "value"));
        assertFalse(errors.isEmpty());
    }

    @Test
    void shouldPassWhenPayloadMatchesSchema() {
        Map<String, Object> schema = Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "required", List.of("region"),
                "additionalProperties", false,
                "properties", Map.of(
                        "region", Map.of("type", "string")
                )
        );

        List<String> errors = validationService.validate(schema, Map.of("region", "GB"));
        assertTrue(errors.isEmpty());
    }
}
