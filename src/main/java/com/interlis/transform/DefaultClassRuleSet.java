package com.interlis.transform;

import java.util.List;
import java.util.Objects;

public final class DefaultClassRuleSet implements ClassRuleSet {
    private final List<Processor> processors;

    public DefaultClassRuleSet(List<Processor> processors) {
        this.processors = List.copyOf(Objects.requireNonNull(processors, "processors"));
    }

    @Override
    public List<Processor> processors() {
        return processors;
    }
}
