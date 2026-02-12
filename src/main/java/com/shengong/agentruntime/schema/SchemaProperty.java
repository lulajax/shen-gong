package com.shengong.agentruntime.schema;

import lombok.Data;

/**
 * Single schema property description.
 */
@Data
public class SchemaProperty {

    private String type;
    private String description;
    private String example;

    public static SchemaProperty of(String type, String description, String example) {
        SchemaProperty property = new SchemaProperty();
        property.setType(type);
        property.setDescription(description);
        property.setExample(example);
        return property;
    }
}
