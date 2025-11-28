package com.shengong.agentruntime.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON Map 转换器
 * 用于将 Map<String, Object> 转换为 JSON 字符串存储到数据库 TEXT 字段
 * 兼容 MySQL 5.6（不支持 JSON 类型）
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Failed to convert Map to JSON string", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to convert JSON string to Map: {}", dbData, e);
            return new HashMap<>();
        }
    }
}

