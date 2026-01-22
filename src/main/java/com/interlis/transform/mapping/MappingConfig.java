package com.interlis.transform.mapping;

import java.util.List;

public final class MappingConfig {
    private List<MappingRule> mappings;

    public List<MappingRule> getMappings() {
        return mappings;
    }

    public void setMappings(List<MappingRule> mappings) {
        this.mappings = mappings;
    }
}
