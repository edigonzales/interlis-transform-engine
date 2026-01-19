package com.interlis.transform;

import ch.interlis.iom.IomObject;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox.EndTransferEvent;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public final class DefaultTransformationEngine implements TransformationEngine {
    private final ExpressionEngine expressionEngine;
    private final StateStore stateStore;
    private final Logger logger;

    public DefaultTransformationEngine(ExpressionEngine expressionEngine, StateStore stateStore, Logger logger) {
        this.expressionEngine = expressionEngine;
        this.stateStore = stateStore;
        this.logger = logger;
    }

    @Override
    public void run(IoxReader reader, IoxWriter writer, TransformationPlan plan) throws Exception {
        TargetEmitter emitter = new WriterTargetEmitter(writer);
        IoxEvent event;
        while ((event = reader.read()) != null) {
            if (!(event instanceof ObjectEvent)) {
                writer.write(event);
                if (event instanceof EndTransferEvent) {
                    break;
                }
                continue;
            }
            IomObject source = ((ObjectEvent) event).getIomObject();
            Optional<ClassRuleSet> rules = plan.rulesFor(source.getobjecttag());
            if (rules.isEmpty()) {
                continue;
            }
            DefaultTransformationContext context = new DefaultTransformationContext(
                    source,
                    emitter,
                    expressionEngine,
                    stateStore,
                    logger
            );
            List<Processor> processors = rules.get().processors();
            for (Processor processor : processors) {
                processor.apply(context);
                if (context.shouldSkip()) {
                    break;
                }
            }
        }
        writer.close();
    }
}
