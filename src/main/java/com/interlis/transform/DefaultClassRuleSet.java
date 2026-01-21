package com.interlis.transform;

import java.util.List;
import java.util.Objects;

public final class DefaultClassRuleSet implements TargetClassRuleSet {
    private final List<Processor> processors;
    private final String targetClassName;

    public DefaultClassRuleSet(String targetClassName, List<Processor> processors) {
        this.targetClassName = Objects.requireNonNull(targetClassName, "targetClassName");
        this.processors = List.copyOf(Objects.requireNonNull(processors, "processors"));
    }

    @Override
    public List<Processor> processors() {
        return processors;
    }

    @Override
    public String targetClassName() {
        return targetClassName;
    }
}
