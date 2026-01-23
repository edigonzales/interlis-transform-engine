package com.interlis.transform.engine;

import com.interlis.transform.TransformationPlan;
import com.interlis.transform.rules.ClassRuleSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SimpleTransformationPlan implements TransformationPlan {
    private final Map<String, ClassRuleSet> rules;
    private final String basketIdStrategy;

    public SimpleTransformationPlan(Map<String, ClassRuleSet> rules, String basketIdStrategy) {
        this.rules = Map.copyOf(Objects.requireNonNull(rules, "rules"));
        this.basketIdStrategy = basketIdStrategy;
    }

    @Override
    public Optional<ClassRuleSet> rulesFor(String sourceClassName) {
        return Optional.ofNullable(rules.get(sourceClassName));
    }

    @Override
    public Set<String> sourceClassNames() {
        return rules.keySet();
    }

    @Override
    public Optional<String> basketIdStrategy() {
        return Optional.ofNullable(basketIdStrategy);
    }
}
