package com.interlis.transform.processor;

import ch.interlis.iom.IomObject;
import com.interlis.transform.Processor;
import com.interlis.transform.TransformationContext;

public final class EmitProcessor implements Processor {
    @Override
    public void apply(TransformationContext ctx) {
        IomObject target = ctx.target().orElseThrow(() -> new IllegalStateException("Target object not initialized"));
        ctx.emitter().emit(target);
    }
}
