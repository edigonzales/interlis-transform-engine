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
            if (!typeSystem.classExists(source.getSourceClass()) && !typeSystem.associationExists(source.getSourceClass())) {
                throw new IllegalArgumentException("Unknown source class or association: " + source.getSourceClass());
            }
        }
        if (!typeSystem.classExists(mapping.getTargetClass()) && !typeSystem.associationExists(mapping.getTargetClass())) {
            throw new IllegalArgumentException("Unknown target class or association: " + mapping.getTargetClass());
        }
        if (mapping.getOidStrategy() != null && SUPPORTED_ID_STRATEGIES.stream()
                .noneMatch(strategy -> strategy.equalsIgnoreCase(mapping.getOidStrategy()))) {
            throw new IllegalArgumentException("Unknown oidStrategy: " + mapping.getOidStrategy());
        }
        if (mapping.getAttributes() != null) {
            for (AttributeMapping attributeMapping : mapping.getAttributes()) {
                if (!typeSystem.attributePathExists(mapping.getTargetClass(), attributeMapping.getTarget())) {
                    throw new IllegalArgumentException("Unknown target attribute path: " + mapping.getTargetClass() + "." + attributeMapping.getTarget());
                }
                validateExpression(mapping, attributeMapping.getExpr(), "attribute mapping '" + attributeMapping.getTarget() + "'");
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
                validateExpression(mapping, joinSpec.getExpr(), "join expression");
            }
        }
        if (mapping.getFilters() != null) {
            for (FilterRule filterRule : mapping.getFilters()) {
                validateExpression(mapping, filterRule.getExpr(), "filter expression");
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

    private void validateExpression(TargetMapping mapping, String expression, String contextLabel) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        Matcher matcher = SOURCE_ATTR_PATTERN.matcher(expression);
        while (matcher.find()) {
            String attr = matcher.group("attr");
            String alias = matcher.group("alias");
            SourceSpec source = resolveSource(mapping, alias);
            if (!typeSystem.attributePathExists(source.getSourceClass(), attr)) {
                throw new IllegalArgumentException("Unknown source attribute path: " + source.getSourceClass()
                        + "." + attr + " in " + contextLabel + " for target class " + mapping.getTargetClass());
            }
        }
        for (RefCall refCall : extractRefCalls(expression)) {
            if (refCall.role() == null) {
                continue;
            }
            SourceSpec source = resolveSource(mapping, refCall.alias());
            if (!typeSystem.roleExists(source.getSourceClass(), refCall.role())) {
                throw new IllegalArgumentException("Unknown role '" + refCall.role() + "' on source class "
                        + source.getSourceClass() + " in " + contextLabel + " for target class " + mapping.getTargetClass());
            }
        }
    }

    private List<RefCall> extractRefCalls(String expression) {
        List<RefCall> calls = new ArrayList<>();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            int openParen = refCallStart(expression, i);
            if (openParen != -1) {
                int startArgs = openParen + 1;
                int depth = 1;
                int end = -1;
                boolean innerSingle = false;
                boolean innerDouble = false;
                for (int j = startArgs; j < expression.length(); j++) {
                    char inner = expression.charAt(j);
                    if (inner == '\'' && !innerDouble) {
                        innerSingle = !innerSingle;
                    } else if (inner == '"' && !innerSingle) {
                        innerDouble = !innerDouble;
                    } else if (!innerSingle && !innerDouble) {
                        if (inner == '(') {
                            depth++;
                        } else if (inner == ')') {
                            depth--;
                            if (depth == 0) {
                                end = j;
                                break;
                            }
                        }
                    }
                }
                if (end > startArgs) {
                    String argsSection = expression.substring(startArgs, end);
                    List<String> args = splitTopLevel(argsSection, ',');
                    String alias = null;
                    String role = null;
                    if (args.size() == 1) {
                        role = normalizeToken(args.get(0));
                    } else if (args.size() >= 2) {
                        alias = normalizeToken(args.get(0));
                        role = normalizeToken(args.get(1));
                    }
                    if (role != null && !role.isBlank()) {
                        calls.add(new RefCall(alias, role));
                    }
                }
                i = openParen;
            }
        }
        return calls;
    }

    private boolean isFunctionBoundary(String expression, int index) {
        if (index == 0) {
            return true;
        }
        char before = expression.charAt(index - 1);
        return !Character.isLetterOrDigit(before) && before != '_';
    }

    private int refCallStart(String expression, int index) {
        if (!expression.regionMatches(true, index, "ref", 0, 3)) {
            return -1;
        }
        if (!isFunctionBoundary(expression, index)) {
            return -1;
        }
        int j = index + 3;
        while (j < expression.length() && Character.isWhitespace(expression.charAt(j))) {
            j++;
        }
        if (j < expression.length() && expression.charAt(j) == '(') {
            return j;
        }
        return -1;
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.contains("${") || trimmed.contains("(") || trimmed.contains(")")) {
            return null;
        }
        return trimmed;
    }

    private List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                }
            }
            if (ch == delimiter && depth == 0 && !inSingleQuote && !inDoubleQuote) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        parts.add(current.toString());
        return parts;
    }

    private record RefCall(String alias, String role) {}
}
