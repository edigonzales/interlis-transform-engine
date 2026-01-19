package com.interlis.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MappingCompiler {
    private static final Pattern SOURCE_ATTR_PATTERN = Pattern.compile("\\$\\{src\\.(?<attr>[^}]+)}");
    private final TypeSystem typeSystem;

    public MappingCompiler(TypeSystem typeSystem) {
        this.typeSystem = Objects.requireNonNull(typeSystem, "typeSystem");
    }

    public TransformationPlan compile(MappingConfig config) {
        Map<String, ClassRuleSet> rules = new HashMap<>();
        if (config.getMappings() == null) {
            return new SimpleTransformationPlan(rules);
        }
        for (MappingRule rule : config.getMappings()) {
            validate(rule);
            List<Processor> processors = new ArrayList<>();
            if (rule.getFilters() != null) {
                for (FilterRule filter : rule.getFilters()) {
                    processors.add(new FilterProcessor(filter.getExpr()));
                }
            }
            processors.add(new CreateTargetObjectProcessor(rule.getTargetClass()));
            if (rule.getAttributes() != null) {
                for (AttributeMapping mapping : rule.getAttributes()) {
                    processors.add(new MapAttributeProcessor(mapping.getTarget(), mapping.getExpr()));
                }
            }
            processors.add(new EmitProcessor());
            rules.put(rule.getSourceClass(), new DefaultClassRuleSet(processors));
        }
        return new SimpleTransformationPlan(rules);
    }

    private void validate(MappingRule rule) {
        if (!typeSystem.classExists(rule.getSourceClass())) {
            throw new IllegalArgumentException("Unknown source class: " + rule.getSourceClass());
        }
        if (!typeSystem.classExists(rule.getTargetClass())) {
            throw new IllegalArgumentException("Unknown target class: " + rule.getTargetClass());
        }
        if (rule.getAttributes() != null) {
            for (AttributeMapping mapping : rule.getAttributes()) {
                if (!typeSystem.attributeExists(rule.getTargetClass(), mapping.getTarget())) {
                    throw new IllegalArgumentException("Unknown target attribute: " + rule.getTargetClass() + "." + mapping.getTarget());
                }
                Matcher matcher = SOURCE_ATTR_PATTERN.matcher(mapping.getExpr());
                while (matcher.find()) {
                    String attr = matcher.group("attr");
                    if (!typeSystem.attributeExists(rule.getSourceClass(), attr)) {
                        throw new IllegalArgumentException("Unknown source attribute: " + rule.getSourceClass() + "." + attr);
                    }
                }
            }
        }
    }
}
