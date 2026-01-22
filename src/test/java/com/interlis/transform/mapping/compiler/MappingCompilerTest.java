package com.interlis.transform.mapping.compiler;

import com.interlis.transform.TransformationPlan;
import com.interlis.transform.TypeSystem;
import com.interlis.transform.mapping.AttributeMapping;
import com.interlis.transform.mapping.MappingConfig;
import com.interlis.transform.mapping.MappingRule;
import com.interlis.transform.rules.ClassRuleSet;
import com.interlis.transform.typesystem.InMemoryTypeSystem;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MappingCompilerTest {
    @Test
    void compilesMappingIntoPlan() {
        MappingConfig config = new MappingConfig();
        MappingRule rule = new MappingRule();
        rule.setSourceClass("ModelA.Foo");
        rule.setTargetClass("ModelB.Bar");
        AttributeMapping attr = new AttributeMapping();
        attr.setTarget("y");
        attr.setExpr("${src.x}");
        rule.setAttributes(List.of(attr));
        config.setMappings(List.of(rule));

        TypeSystem typeSystem = new InMemoryTypeSystem()
                .registerClass("ModelA.Foo", Set.of("x"))
                .registerClass("ModelB.Bar", Set.of("y"));

        TransformationPlan plan = new MappingCompiler(typeSystem).compile(config);

        ClassRuleSet ruleSet = plan.rulesFor("ModelA.Foo").orElseThrow();

        assertThat(ruleSet.processors()).hasSize(3);
    }
}
