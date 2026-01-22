package com.interlis.transform.expression;

import java.util.List;

@FunctionalInterface
public interface ExpressionFunction {
    Object apply(List<Object> args);
}
