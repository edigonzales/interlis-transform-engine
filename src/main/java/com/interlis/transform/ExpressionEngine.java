package com.interlis.transform;

public interface ExpressionEngine {
    Object evaluate(String expression, TransformationContext context);
}
