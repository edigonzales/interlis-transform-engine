package com.interlis.transform.expression;

import com.interlis.transform.TransformationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BasicExpressionEngine implements ExpressionEngine {
    private final FunctionRegistry functionRegistry;

    public BasicExpressionEngine(FunctionRegistry functionRegistry) {
        this.functionRegistry = Objects.requireNonNull(functionRegistry, "functionRegistry");
    }

    @Override
    public Object evaluate(String expression, TransformationContext context) {
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.contains("!=")) {
            String[] parts = trimmed.split("!=", 2);
            Object left = evaluate(parts[0].trim(), context);
            Object right = evaluate(parts[1].trim(), context);
            return !Objects.equals(stringify(left), stringify(right));
        }
        if (trimmed.contains("==")) {
            String[] parts = trimmed.split("==", 2);
            Object left = evaluate(parts[0].trim(), context);
            Object right = evaluate(parts[1].trim(), context);
            return Objects.equals(stringify(left), stringify(right));
        }
        List<String> plusParts = splitTopLevel(trimmed, '+');
        if (plusParts.size() > 1) {
            List<Object> args = new ArrayList<>();
            for (String part : plusParts) {
                args.add(evaluate(part.trim(), context));
            }
            return functionRegistry.invoke("add", args);
        }
        if (isStringLiteral(trimmed)) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (isNumber(trimmed)) {
            return trimmed;
        }
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            String token = trimmed.substring(2, trimmed.length() - 1);
            return resolvePath(token.trim(), context);
        }
        if (trimmed.contains("(") && trimmed.endsWith(")")) {
            int idx = trimmed.indexOf('(');
            String name = trimmed.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String argsSection = trimmed.substring(idx + 1, trimmed.length() - 1);
            List<String> argsTokens = splitTopLevel(argsSection, ',');
            List<Object> args = new ArrayList<>();
            for (String arg : argsTokens) {
                if (!arg.isBlank()) {
                    args.add(evaluate(arg.trim(), context));
                }
            }
            return functionRegistry.invoke(name, args);
        }
        return trimmed;
    }

    private Object resolvePath(String token, TransformationContext context) {
        if (token.startsWith("src.")) {
            String attr = token.substring("src.".length());
            return context.source().getattrvalue(attr);
        }
        if (token.startsWith("state.")) {
            String key = token.substring("state.".length());
            return context.state().getTargetOid(key).orElse(null);
        }
        return null;
    }

    private boolean isStringLiteral(String value) {
        return (value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"));
    }

    private boolean isNumber(String value) {
        return value.matches("-?\\d+(\\.\\d+)?");
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

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
