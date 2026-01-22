package com.interlis.transform.expression;

import com.interlis.transform.TransformationContext;

public interface ExpressionEngine {
    Object evaluate(String expression, TransformationContext context);
}
