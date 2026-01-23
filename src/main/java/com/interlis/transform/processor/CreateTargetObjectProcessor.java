package com.interlis.transform.processor;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import com.interlis.transform.Processor;
import com.interlis.transform.TransformationContext;
import java.util.Objects;
import java.util.UUID;

public final class CreateTargetObjectProcessor implements Processor {
    private final String targetClass;
    private final String oidStrategy;

    public CreateTargetObjectProcessor(String targetClass, String oidStrategy) {
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass");
        this.oidStrategy = oidStrategy;
    }

    @Override
    public void apply(TransformationContext ctx) {
        String oid = null;
        if ("preserve".equalsIgnoreCase(oidStrategy)) {
            oid = ctx.source().getobjectoid();
        } else if ("uuid".equalsIgnoreCase(oidStrategy) || "generate".equalsIgnoreCase(oidStrategy)) {
            oid = UUID.randomUUID().toString();
        }
        IomObject target = new Iom_jObject(targetClass, oid);
        ctx.target(target);
    }
}
