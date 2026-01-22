package com.interlis.transform;

import com.interlis.transform.rules.ClassRuleSet;
import java.util.Optional;
import java.util.Set;

public interface TransformationPlan {
    Optional<ClassRuleSet> rulesFor(String sourceClassName);

    Set<String> sourceClassNames();
}
