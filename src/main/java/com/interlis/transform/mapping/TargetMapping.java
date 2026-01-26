package com.interlis.transform.mapping;

import java.util.List;

public final class TargetMapping {
    private String targetClass;
    private String oidStrategy;
    private List<SourceSpec> sources;
    private List<JoinSpec> joins;
    private List<AttributeMapping> attributes;
    private List<FilterRule> filters;

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

    public List<SourceSpec> getSources() {
        return sources;
    }

    public void setSources(List<SourceSpec> sources) {
        this.sources = sources;
    }

    public List<JoinSpec> getJoins() {
        return joins;
    }

    public void setJoins(List<JoinSpec> joins) {
        this.joins = joins;
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
