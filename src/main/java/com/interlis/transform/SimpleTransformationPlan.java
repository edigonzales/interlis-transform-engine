package com.interlis.transform;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SimpleTransformationPlan implements TransformationPlan {
    private final Map<String, ClassRuleSet> rules;

    public SimpleTransformationPlan(Map<String, ClassRuleSet> rules) {
        this.rules = Map.copyOf(Objects.requireNonNull(rules, "rules"));
    }

    @Override
    public Optional<ClassRuleSet> rulesFor(String sourceClassName) {
        return Optional.ofNullable(rules.get(sourceClassName));
    }

    @Override
    public Set<String> sourceClassNames() {
        return rules.keySet();
    }
}
