package com.interlis.transform.mapping.compiler;

import com.interlis.transform.Processor;
import com.interlis.transform.TransformationPlan;
import com.interlis.transform.TypeSystem;
import com.interlis.transform.engine.SimpleTransformationPlan;
import com.interlis.transform.mapping.AttributeMapping;
import com.interlis.transform.mapping.FilterRule;
import com.interlis.transform.mapping.MappingConfig;
import com.interlis.transform.mapping.SourceSpec;
import com.interlis.transform.mapping.TargetMapping;
import com.interlis.transform.processor.CreateTargetObjectProcessor;
import com.interlis.transform.processor.EmitProcessor;
import com.interlis.transform.processor.FilterProcessor;
import com.interlis.transform.processor.MapAttributeProcessor;
import com.interlis.transform.rules.DefaultClassRuleSet;
import com.interlis.transform.rules.TargetClassRuleSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MappingCompiler {
    private static final Pattern SOURCE_ATTR_PATTERN =
            Pattern.compile("\\$\\{src(?:\\.(?<alias>[^.}]+))?\\.(?<attr>[^}]+)}");
    private static final List<String> SUPPORTED_ID_STRATEGIES = List.of("preserve", "uuid", "generate", "integer");
    private final TypeSystem typeSystem;

    public MappingCompiler(TypeSystem typeSystem) {
        this.typeSystem = Objects.requireNonNull(typeSystem, "typeSystem");
    }

    public TransformationPlan compile(MappingConfig config) {
        Map<String, List<TargetClassRuleSet>> rules = new HashMap<>();
        validateBasketStrategy(config.getBasketIdStrategy());
        if (config.getMappings() == null) {
            return new SimpleTransformationPlan(rules, config.getBasketIdStrategy());
        }
        for (TargetMapping mapping : config.getMappings()) {
            validate(mapping);
            List<Processor> processors = new ArrayList<>();
            if (mapping.getFilters() != null) {
                for (FilterRule filter : mapping.getFilters()) {
                    processors.add(new FilterProcessor(filter.getExpr()));
                }
            }
            processors.add(new CreateTargetObjectProcessor(mapping.getTargetClass(), mapping.getOidStrategy()));
            if (mapping.getAttributes() != null) {
                for (AttributeMapping attributeMapping : mapping.getAttributes()) {
                    processors.add(new MapAttributeProcessor(attributeMapping.getTarget(), attributeMapping.getExpr()));
                }
            }
            processors.add(new EmitProcessor());
            SourceSpec primarySource = mapping.getSources().get(0);
            TargetClassRuleSet ruleSet = new DefaultClassRuleSet(
                    mapping.getTargetClass(),
                    processors,
                    mapping.getSources(),
                    mapping.getJoins(),
                    primarySource
            );
            rules.computeIfAbsent(primarySource.getSourceClass(), key -> new ArrayList<>()).add(ruleSet);
        }
        return new SimpleTransformationPlan(rules, config.getBasketIdStrategy());
    }

    private void validate(TargetMapping mapping) {
        List<SourceSpec> sources = mapping.getSources();
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("No sources defined for target class: " + mapping.getTargetClass());
        }
        if (sources.size() > 1) {
            for (SourceSpec source : sources) {
                if (source.getAlias() == null || source.getAlias().isBlank()) {
                    throw new IllegalArgumentException("Missing alias for multi-source mapping to " + mapping.getTargetClass());
                }
            }
        }
        for (SourceSpec source : sources) {
            if (!typeSystem.classExists(source.getSourceClass())) {
                throw new IllegalArgumentException("Unknown source class: " + source.getSourceClass());
            }
        }
        if (!typeSystem.classExists(mapping.getTargetClass())) {
            throw new IllegalArgumentException("Unknown target class: " + mapping.getTargetClass());
        }
        if (mapping.getOidStrategy() != null && SUPPORTED_ID_STRATEGIES.stream()
                .noneMatch(strategy -> strategy.equalsIgnoreCase(mapping.getOidStrategy()))) {
            throw new IllegalArgumentException("Unknown oidStrategy: " + mapping.getOidStrategy());
        }
        if (mapping.getAttributes() != null) {
            for (AttributeMapping attributeMapping : mapping.getAttributes()) {
                if (!typeSystem.attributeExists(mapping.getTargetClass(), attributeMapping.getTarget())) {
                    throw new IllegalArgumentException("Unknown target attribute: " + mapping.getTargetClass() + "." + attributeMapping.getTarget());
                }
                Matcher matcher = SOURCE_ATTR_PATTERN.matcher(attributeMapping.getExpr());
                while (matcher.find()) {
                    String attr = matcher.group("attr");
                    String alias = matcher.group("alias");
                    SourceSpec source = resolveSource(mapping, alias);
                    if (!typeSystem.attributeExists(source.getSourceClass(), attr)) {
                        throw new IllegalArgumentException("Unknown source attribute: " + source.getSourceClass() + "." + attr);
                    }
                }
            }
        }
        if (mapping.getJoins() != null) {
            for (com.interlis.transform.mapping.JoinSpec joinSpec : mapping.getJoins()) {
                if (joinSpec.getLeftAlias() == null || joinSpec.getLeftAlias().isBlank()) {
                    throw new IllegalArgumentException("Join is missing leftAlias for target class " + mapping.getTargetClass());
                }
                if (joinSpec.getRightAlias() == null || joinSpec.getRightAlias().isBlank()) {
                    throw new IllegalArgumentException("Join is missing rightAlias for target class " + mapping.getTargetClass());
                }
                resolveSource(mapping, joinSpec.getLeftAlias());
                resolveSource(mapping, joinSpec.getRightAlias());
            }
        }
    }

    private SourceSpec resolveSource(TargetMapping mapping, String alias) {
        if (alias == null || alias.isBlank()) {
            if (mapping.getSources().size() == 1) {
                return mapping.getSources().get(0);
            }
            throw new IllegalArgumentException("Attribute references must include an alias for multi-source mapping to " + mapping.getTargetClass());
        }
        for (SourceSpec source : mapping.getSources()) {
            if (alias.equals(source.getAlias())) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown source alias '" + alias + "' for target class " + mapping.getTargetClass());
    }

    private void validateBasketStrategy(String basketIdStrategy) {
        if (basketIdStrategy != null && SUPPORTED_ID_STRATEGIES.stream()
                .noneMatch(strategy -> strategy.equalsIgnoreCase(basketIdStrategy))) {
            throw new IllegalArgumentException("Unknown basketIdStrategy: " + basketIdStrategy);
        }
    }
}
