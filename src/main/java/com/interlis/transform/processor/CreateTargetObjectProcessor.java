package com.interlis.transform.processor;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import com.interlis.transform.Processor;
import com.interlis.transform.TransformationContext;
import java.util.Objects;

public final class CreateTargetObjectProcessor implements Processor {
    private final String targetClass;

    public CreateTargetObjectProcessor(String targetClass) {
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass");
    }

    @Override
    public void apply(TransformationContext ctx) {
        IomObject target = new Iom_jObject(targetClass, null);
        ctx.target(target);
    }
}
