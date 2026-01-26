package com.interlis.transform.mapping;

import java.util.List;

public final class MappingConfig {
    private List<TargetMapping> mappings;
    private String basketIdStrategy;

    public List<TargetMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<TargetMapping> mappings) {
        this.mappings = mappings;
    }

    public String getBasketIdStrategy() {
        return basketIdStrategy;
    }

    public void setBasketIdStrategy(String basketIdStrategy) {
        this.basketIdStrategy = basketIdStrategy;
    }
}
