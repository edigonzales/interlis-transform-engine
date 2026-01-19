package com.interlis.transform;

import java.util.Optional;

public interface TransformationPlan {
    Optional<ClassRuleSet> rulesFor(String sourceClassName);
}
