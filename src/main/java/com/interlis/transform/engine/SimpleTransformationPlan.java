package com.interlis.transform.engine;

import com.interlis.transform.TransformationPlan;
import com.interlis.transform.rules.TargetClassRuleSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SimpleTransformationPlan implements TransformationPlan {
    private final Map<String, List<TargetClassRuleSet>> rules;
    private final String basketIdStrategy;

    public SimpleTransformationPlan(Map<String, List<TargetClassRuleSet>> rules, String basketIdStrategy) {
        this.rules = Map.copyOf(Objects.requireNonNull(rules, "rules"));
        this.basketIdStrategy = basketIdStrategy;
    }

    @Override
    public List<TargetClassRuleSet> rulesFor(String sourceClassName) {
        List<TargetClassRuleSet> ruleSets = rules.get(sourceClassName);
        return ruleSets == null ? List.of() : Collections.unmodifiableList(ruleSets);
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
