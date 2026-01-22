package com.interlis.transform.mapping;

import java.util.List;

public final class MappingConfig {
    private List<MappingRule> mappings;
    private String basketIdStrategy;

    public List<MappingRule> getMappings() {
        return mappings;
    }

    public void setMappings(List<MappingRule> mappings) {
        this.mappings = mappings;
    }

    public String getBasketIdStrategy() {
        return basketIdStrategy;
    }

    public void setBasketIdStrategy(String basketIdStrategy) {
        this.basketIdStrategy = basketIdStrategy;
    }
}
