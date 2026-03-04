package com.shengong.agentruntime.service.runtime.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON Schema 校验服务，用于动态参数的严格校验。
 * 支持 JSON Schema 2020-12 规范，基于 networknt 校验库。
 */
@Service
public class JsonSchemaValidationService {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public JsonSchemaValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    public List<String> validate(Map<String, Object> schemaMap, Map<String, Object> payload) {
        if (schemaMap == null || schemaMap.isEmpty()) {
            return List.of();
        }

        try {
            JsonNode schemaNode = objectMapper.valueToTree(schemaMap);
            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            JsonNode payloadNode = objectMapper.valueToTree(
                    payload != null ? payload : Collections.emptyMap());

            Set<ValidationMessage> violations = schema.validate(payloadNode);
            if (violations.isEmpty()) {
                return List.of();
            }

            List<String> errors = new ArrayList<>();
            for (ValidationMessage violation : violations) {
                errors.add(violation.getMessage());
            }
            return errors;
        } catch (Exception e) {
            return List.of("Schema validation failed: " + e.getMessage());
        }
    }
}
