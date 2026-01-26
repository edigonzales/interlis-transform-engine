package com.interlis.transform;

import ch.interlis.iom.IomObject;
import com.interlis.transform.emit.TargetEmitter;
import com.interlis.transform.expression.ExpressionEngine;
import com.interlis.transform.state.StateStore;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public interface TransformationContext {
    IomObject source();

    Optional<IomObject> source(String alias);

    Map<String, IomObject> sources();

    Optional<IomObject> target();

    void target(IomObject target);

    TargetEmitter emitter();

    ExpressionEngine expr();

    StateStore state();

    Logger log();

    boolean shouldSkip();

    void skip();
}
