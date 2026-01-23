package com.interlis.transform.mapping.compiler;

import com.interlis.transform.Processor;
import com.interlis.transform.TransformationPlan;
import com.interlis.transform.TypeSystem;
import com.interlis.transform.engine.SimpleTransformationPlan;
import com.interlis.transform.mapping.AttributeMapping;
import com.interlis.transform.mapping.FilterRule;
import com.interlis.transform.mapping.MappingConfig;
import com.interlis.transform.mapping.MappingRule;
import com.interlis.transform.processor.CreateTargetObjectProcessor;
import com.interlis.transform.processor.EmitProcessor;
import com.interlis.transform.processor.FilterProcessor;
import com.interlis.transform.processor.MapAttributeProcessor;
import com.interlis.transform.rules.ClassRuleSet;
import com.interlis.transform.rules.DefaultClassRuleSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MappingCompiler {
    private static final Pattern SOURCE_ATTR_PATTERN = Pattern.compile("\\$\\{src\\.(?<attr>[^}]+)}");
    private static final List<String> SUPPORTED_ID_STRATEGIES = List.of("preserve", "uuid", "generate", "integer");
    private final TypeSystem typeSystem;

    public MappingCompiler(TypeSystem typeSystem) {
        this.typeSystem = Objects.requireNonNull(typeSystem, "typeSystem");
    }

    public TransformationPlan compile(MappingConfig config) {
        Map<String, ClassRuleSet> rules = new HashMap<>();
        validateBasketStrategy(config.getBasketIdStrategy());
        if (config.getMappings() == null) {
            return new SimpleTransformationPlan(rules, config.getBasketIdStrategy());
        }
        for (MappingRule rule : config.getMappings()) {
            validate(rule);
            List<Processor> processors = new ArrayList<>();
            if (rule.getFilters() != null) {
                for (FilterRule filter : rule.getFilters()) {
                    processors.add(new FilterProcessor(filter.getExpr()));
                }
            }
            processors.add(new CreateTargetObjectProcessor(rule.getTargetClass(), rule.getOidStrategy()));
            if (rule.getAttributes() != null) {
                for (AttributeMapping mapping : rule.getAttributes()) {
                    processors.add(new MapAttributeProcessor(mapping.getTarget(), mapping.getExpr()));
                }
            }
            processors.add(new EmitProcessor());
            rules.put(rule.getSourceClass(), new DefaultClassRuleSet(rule.getTargetClass(), processors));
        }
        return new SimpleTransformationPlan(rules, config.getBasketIdStrategy());
    }

    private void validate(MappingRule rule) {
        if (!typeSystem.classExists(rule.getSourceClass())) {
            throw new IllegalArgumentException("Unknown source class: " + rule.getSourceClass());
        }
        if (!typeSystem.classExists(rule.getTargetClass())) {
            throw new IllegalArgumentException("Unknown target class: " + rule.getTargetClass());
        }
        if (rule.getOidStrategy() != null && SUPPORTED_ID_STRATEGIES.stream()
                .noneMatch(strategy -> strategy.equalsIgnoreCase(rule.getOidStrategy()))) {
            throw new IllegalArgumentException("Unknown oidStrategy: " + rule.getOidStrategy());
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

    private void validateBasketStrategy(String basketIdStrategy) {
        if (basketIdStrategy != null && SUPPORTED_ID_STRATEGIES.stream()
                .noneMatch(strategy -> strategy.equalsIgnoreCase(basketIdStrategy))) {
            throw new IllegalArgumentException("Unknown basketIdStrategy: " + basketIdStrategy);
        }
    }
}
