package com.interlis.transform;

import com.interlis.transform.rules.TargetClassRuleSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TransformationPlan {
    List<TargetClassRuleSet> rulesFor(String sourceClassName);

    Set<String> sourceClassNames();

    Optional<String> basketIdStrategy();
}
