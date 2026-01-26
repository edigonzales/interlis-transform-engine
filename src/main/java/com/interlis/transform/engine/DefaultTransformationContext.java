package com.interlis.transform.engine;

import ch.interlis.iom.IomObject;
import com.interlis.transform.RoleResolver;
import com.interlis.transform.TransformationContext;
import com.interlis.transform.emit.TargetEmitter;
import com.interlis.transform.expression.ExpressionEngine;
import com.interlis.transform.state.StateStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class DefaultTransformationContext implements TransformationContext {
    private final Map<String, IomObject> sources;
    private final String primaryAlias;
    private final TargetEmitter emitter;
    private final ExpressionEngine expressionEngine;
    private final StateStore stateStore;
    private final Logger logger;
    private final String basketId;
    private final RoleResolver roleResolver;
    private IomObject target;
    private boolean skip;

    public DefaultTransformationContext(
            Map<String, IomObject> sources,
            String primaryAlias,
            String basketId,
            TargetEmitter emitter,
            ExpressionEngine expressionEngine,
            StateStore stateStore,
            Logger logger,
            RoleResolver roleResolver
    ) {
        this.sources = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(sources, "sources")));
        this.primaryAlias = Objects.requireNonNull(primaryAlias, "primaryAlias");
        this.basketId = basketId;
        this.emitter = Objects.requireNonNull(emitter, "emitter");
        this.expressionEngine = Objects.requireNonNull(expressionEngine, "expressionEngine");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.roleResolver = Objects.requireNonNull(roleResolver, "roleResolver");
    }

    public DefaultTransformationContext(
            IomObject source,
            TargetEmitter emitter,
            ExpressionEngine expressionEngine,
            StateStore stateStore,
            Logger logger
    ) {
        this(Map.of("src", source), "src", null, emitter, expressionEngine, stateStore, logger, RoleResolver.NONE);
    }

    @Override
    public IomObject source() {
        return sources.get(primaryAlias);
    }

    @Override
    public Optional<IomObject> source(String alias) {
        if (alias == null || alias.isBlank()) {
            return Optional.ofNullable(sources.get(primaryAlias));
        }
        return Optional.ofNullable(sources.get(alias));
    }

    @Override
    public Map<String, IomObject> sources() {
        return sources;
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
    public Optional<String> basketId() {
        return Optional.ofNullable(basketId);
    }

    @Override
    public RoleResolver roleResolver() {
        return roleResolver;
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
