package com.interlis.transform.expression;

import com.interlis.transform.TransformationContext;
import java.util.List;

@FunctionalInterface
public interface ExpressionFunction {
    Object apply(List<Object> args, TransformationContext context);
}
