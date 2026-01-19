package com.interlis.transform;

import ch.interlis.iom.IomObject;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class DefaultTransformationContext implements TransformationContext {
    private final IomObject source;
    private final TargetEmitter emitter;
    private final ExpressionEngine expressionEngine;
    private final StateStore stateStore;
    private final Logger logger;
    private IomObject target;
    private boolean skip;

    public DefaultTransformationContext(
            IomObject source,
            TargetEmitter emitter,
            ExpressionEngine expressionEngine,
            StateStore stateStore,
            Logger logger
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.emitter = Objects.requireNonNull(emitter, "emitter");
        this.expressionEngine = Objects.requireNonNull(expressionEngine, "expressionEngine");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public IomObject source() {
        return source;
    }

    @Override
    public Optional<IomObject> target() {
        return Optional.ofNullable(target);
    }

    @Override
    public void target(IomObject target) {
        this.target = target;
    }

    @Override
    public TargetEmitter emitter() {
        return emitter;
    }

    @Override
    public ExpressionEngine expr() {
        return expressionEngine;
    }

    @Override
    public StateStore state() {
        return stateStore;
    }

    @Override
    public Logger log() {
        return logger;
    }

    @Override
    public boolean shouldSkip() {
        return skip;
    }

    @Override
    public void skip() {
        this.skip = true;
    }
}
