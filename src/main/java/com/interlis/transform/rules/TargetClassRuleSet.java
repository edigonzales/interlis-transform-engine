package com.interlis.transform.rules;

import com.interlis.transform.mapping.JoinSpec;
import com.interlis.transform.mapping.SourceSpec;
import java.util.List;

public interface TargetClassRuleSet extends ClassRuleSet {
    String targetClassName();

    List<SourceSpec> sources();

    List<JoinSpec> joins();

    SourceSpec primarySource();
}
