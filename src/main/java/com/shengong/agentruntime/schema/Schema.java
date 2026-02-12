package com.shengong.agentruntime.schema;

import lombok.Data;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight schema model for tool input/output validation.
 */
@Data
public class Schema {

    private String id;
    private String title;
    private Map<String, SchemaProperty> properties = new LinkedHashMap<>();
    private List<String> required = new ArrayList<>();

    public static Schema empty(String id) {
        Schema schema = new Schema();
        schema.setId(id);
        schema.setTitle(id);
        return schema;
    }

    public static Schema object(String id, String title) {
        Schema schema = new Schema();
        schema.setId(id);
        schema.setTitle(title);
        return schema;
    }

    public Schema addProperty(String name, SchemaProperty property, boolean isRequired) {
        this.properties.put(name, property);
        if (isRequired) {
            this.required.add(name);
        }
        return this;
    }

    public boolean isEmpty() {
        return properties == null || properties.isEmpty();
    }
}
