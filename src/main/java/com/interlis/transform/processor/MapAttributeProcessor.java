package com.interlis.transform.processor;

import ch.interlis.iom.IomObject;
import com.interlis.transform.Processor;
import com.interlis.transform.TransformationContext;
import java.util.Objects;

public final class MapAttributeProcessor implements Processor {
    private final String targetAttribute;
    private final String expression;

    public MapAttributeProcessor(String targetAttribute, String expression) {
        this.targetAttribute = Objects.requireNonNull(targetAttribute, "targetAttribute");
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    @Override
    public void apply(TransformationContext ctx) {
        IomObject target = ctx.target().orElseThrow(() -> new IllegalStateException("Target object not initialized"));
        Object value = ctx.expr().evaluate(expression, ctx);
        if (value != null) {
            if (value instanceof IomObject) {
                int valueCount = target.getattrvaluecount(targetAttribute);
                for (int i = valueCount - 1; i >= 0; i--) {
                    target.deleteattrobj(targetAttribute, i);
                }
                target.addattrobj(targetAttribute, (IomObject) value);
            } else {
                target.setattrvalue(targetAttribute, value.toString());
            }
        }
    }
}
