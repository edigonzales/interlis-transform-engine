package com.interlis.transform.rules;

import com.interlis.transform.Processor;
import com.interlis.transform.mapping.JoinSpec;
import com.interlis.transform.mapping.SourceSpec;
import java.util.List;
import java.util.Objects;

public final class DefaultClassRuleSet implements TargetClassRuleSet {
    private final List<Processor> processors;
    private final String targetClassName;
    private final List<SourceSpec> sources;
    private final List<JoinSpec> joins;
    private final SourceSpec primarySource;

    public DefaultClassRuleSet(
            String targetClassName,
            List<Processor> processors,
            List<SourceSpec> sources,
            List<JoinSpec> joins,
            SourceSpec primarySource
    ) {
        this.targetClassName = Objects.requireNonNull(targetClassName, "targetClassName");
        this.processors = List.copyOf(Objects.requireNonNull(processors, "processors"));
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
        this.joins = joins == null ? List.of() : List.copyOf(joins);
        this.primarySource = Objects.requireNonNull(primarySource, "primarySource");
    }

    @Override
    public List<Processor> processors() {
        return processors;
    }

    @Override
    public String targetClassName() {
        return targetClassName;
    }

    @Override
    public List<SourceSpec> sources() {
        return sources;
    }

    @Override
    public List<JoinSpec> joins() {
        return joins;
    }

    @Override
    public SourceSpec primarySource() {
        return primarySource;
    }
}
