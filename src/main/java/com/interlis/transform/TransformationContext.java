package com.interlis.transform;

import ch.interlis.iom.IomObject;
import java.util.Optional;
import java.util.logging.Logger;

public interface TransformationContext {
    IomObject source();

    Optional<IomObject> target();

    void target(IomObject target);

    TargetEmitter emitter();

    ExpressionEngine expr();

    StateStore state();

    Logger log();

    boolean shouldSkip();

    void skip();
}
