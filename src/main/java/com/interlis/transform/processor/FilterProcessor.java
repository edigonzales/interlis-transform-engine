package com.interlis.transform.processor;

import com.interlis.transform.Processor;
import com.interlis.transform.TransformationContext;
import java.util.Objects;

public final class FilterProcessor implements Processor {
    private final String expression;

    public FilterProcessor(String expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    @Override
    public void apply(TransformationContext ctx) {
        Object value = ctx.expr().evaluate(expression, ctx);
        boolean keep = value instanceof Boolean
                ? (Boolean) value
                : value != null && Boolean.parseBoolean(value.toString());
        if (!keep) {
            ctx.skip();
        }
    }
}
