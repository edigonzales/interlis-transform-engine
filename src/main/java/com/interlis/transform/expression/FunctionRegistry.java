package com.interlis.transform.expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class FunctionRegistry {
    private final Map<String, ExpressionFunction> functions = new HashMap<>();

    public FunctionRegistry() {
        registerDefaults();
    }

    public void register(String name, ExpressionFunction function) {
        functions.put(name, function);
    }

    public ExpressionFunction get(String name) {
        ExpressionFunction function = functions.get(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function: " + name);
        }
        return function;
    }

    private void registerDefaults() {
        register("substring", args -> {
            String value = Objects.toString(args.get(0), "");
            int start = toInt(args.get(1));
            int length = toInt(args.get(2));
            int end = Math.min(value.length(), start + length);
            if (start >= value.length()) {
                return "";
            }
            return value.substring(Math.max(0, start), end);
        });
        register("coalesce", args -> {
            for (Object arg : args) {
                if (arg != null) {
                    String value = arg.toString();
                    if (!value.isBlank()) {
                        return value;
                    }
                }
            }
            return null;
        });
        register("add", args -> {
            double sum = 0;
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                sum += Double.parseDouble(arg.toString());
            }
            if (Math.floor(sum) == sum) {
                return Long.toString((long) sum);
            }
            return Double.toString(sum);
        });
        register("if", args -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("if requires condition, true value, and false value arguments");
            }
            boolean condition = toBoolean(args.get(0));
            return condition ? args.get(1) : args.get(2);
        });
        register("to_xml_date", args -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("to_xml_date requires at least a value argument");
            }
            Object value = args.get(0);
            if (value == null) {
                return null;
            }
            String raw = value.toString().trim();
            if (raw.isEmpty()) {
                return null;
            }
            String pattern = args.size() > 1 ? Objects.toString(args.get(1), "yyyyMMdd") : "yyyyMMdd";
            try {
                LocalDate date = LocalDate.parse(raw, DateTimeFormatter.ofPattern(pattern));
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date value '" + raw + "' for pattern '" + pattern + "'", e);
            }
        });
        register("to_xml_datetime", args -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("to_xml_datetime requires at least a value argument");
            }
            Object value = args.get(0);
            if (value == null) {
                return null;
            }
            String raw = value.toString().trim();
            if (raw.isEmpty()) {
                return null;
            }
            String pattern = args.size() > 1 ? Objects.toString(args.get(1), "yyyyMMdd") : "yyyyMMdd";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            try {
                if (pattern.matches(".*[HhKkms].*")) {
                    return LocalDateTime.parse(raw, formatter).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                LocalDate date = LocalDate.parse(raw, formatter);
                return date.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid datetime value '" + raw + "' for pattern '" + pattern + "'", e);
            }
        });
        register("from_xml_date", args -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("from_xml_date requires at least a value argument");
            }
            Object value = args.get(0);
            if (value == null) {
                return null;
            }
            String raw = value.toString().trim();
            if (raw.isEmpty()) {
                return null;
            }
            String pattern = args.size() > 1 ? Objects.toString(args.get(1), "yyyyMMdd") : "yyyyMMdd";
            try {
                LocalDate date = raw.contains("T")
                        ? LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
                        : LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.format(DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date value '" + raw + "' for ISO date", e);
            }
        });
        register("from_xml_datetime", args -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("from_xml_datetime requires at least a value argument");
            }
            return get("from_xml_date").apply(args);
        });
    }

    private int toInt(Object value) {
        return Integer.parseInt(Objects.toString(value));
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String raw = value.toString().trim();
        if (raw.equalsIgnoreCase("true")) {
            return true;
        }
        if (raw.equalsIgnoreCase("false") || raw.isEmpty()) {
            return false;
        }
        throw new IllegalArgumentException("Unable to interpret '" + raw + "' as boolean");
    }

    public Object invoke(String name, List<Object> args) {
        return get(name).apply(args);
    }
}
