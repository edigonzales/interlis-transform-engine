package com.interlis.transform.mapping;

import java.util.List;

public final class MappingRule {
    private String sourceClass;
    private String targetClass;
    private String oidStrategy;
    private List<AttributeMapping> attributes;
    private List<FilterRule> filters;

    public String getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(String sourceClass) {
        this.sourceClass = sourceClass;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public String getOidStrategy() {
        return oidStrategy;
    }

    public void setOidStrategy(String oidStrategy) {
        this.oidStrategy = oidStrategy;
    }

    public List<AttributeMapping> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeMapping> attributes) {
        this.attributes = attributes;
    }

    public List<FilterRule> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterRule> filters) {
        this.filters = filters;
    }
}
