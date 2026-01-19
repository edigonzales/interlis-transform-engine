package com.interlis.transform;

import ch.interlis.iom.IomObject;

public final class EmitProcessor implements Processor {
    @Override
    public void apply(TransformationContext ctx) {
        IomObject target = ctx.target().orElseThrow(() -> new IllegalStateException("Target object not initialized"));
        ctx.emitter().emit(target);
    }
}
