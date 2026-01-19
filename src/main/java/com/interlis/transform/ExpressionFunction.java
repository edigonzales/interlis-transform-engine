package com.interlis.transform;

import java.util.List;

@FunctionalInterface
public interface ExpressionFunction {
    Object apply(List<Object> args);
}
