package com.interlis.transform.engine;

import ch.interlis.iom.IomObject;
import ch.interlis.iox.EndBasketEvent;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox.StartBasketEvent;
import ch.interlis.iox.StartTransferEvent;
import com.interlis.transform.Processor;
import com.interlis.transform.TransformationEngine;
import com.interlis.transform.TransformationPlan;
import com.interlis.transform.emit.TargetEmitter;
import com.interlis.transform.emit.WriterTargetEmitter;
import com.interlis.transform.expression.ExpressionEngine;
import com.interlis.transform.rules.ClassRuleSet;
import com.interlis.transform.rules.TargetClassRuleSet;
import com.interlis.transform.state.StateStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Map<String, String> topicMapping = buildTopicMapping(plan);
        boolean writeBasket = false;
        IoxEvent event;
        while ((event = reader.read()) != null) {
            if (event instanceof StartTransferEvent) {
                StartTransferEvent start = (StartTransferEvent) event;
                writer.write(new ch.interlis.iox_j.StartTransferEvent(start.getSender(), start.getComment(), start.getVersion()));
                continue;
            }
            if (event instanceof StartBasketEvent) {
                StartBasketEvent basket = (StartBasketEvent) event;
                String targetTopic = topicMapping.get(basket.getType());
                if (targetTopic != null) {
                    writer.write(new ch.interlis.iox_j.StartBasketEvent(targetTopic, basket.getBid()));
                    writeBasket = true;
                } else {
                    writeBasket = false;
                }
                continue;
            }
            if (event instanceof ObjectEvent) {
                if (!writeBasket) {
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
                continue;
            }
            if (event instanceof EndBasketEvent) {
                if (writeBasket) {
                    writer.write(new ch.interlis.iox_j.EndBasketEvent());
                }
                writeBasket = false;
                continue;
            }
            if (event instanceof EndTransferEvent) {
                writer.write(new ch.interlis.iox_j.EndTransferEvent());
                break;
            }
        }
        writer.close();
    }

    private Map<String, String> buildTopicMapping(TransformationPlan plan) {
        Map<String, String> mapping = new HashMap<>();
        for (String sourceClass : plan.sourceClassNames()) {
            Optional<ClassRuleSet> rules = plan.rulesFor(sourceClass);
            if (rules.isEmpty()) {
                continue;
            }
            String targetClass = null;
            ClassRuleSet ruleSet = rules.get();
            if (ruleSet instanceof TargetClassRuleSet) {
                targetClass = ((TargetClassRuleSet) ruleSet).targetClassName();
            }
            if (targetClass == null) {
                continue;
            }
            String sourceTopic = extractTopicName(sourceClass);
            String targetTopic = extractTopicName(targetClass);
            if (sourceTopic != null && targetTopic != null) {
                mapping.putIfAbsent(sourceTopic, targetTopic);
            }
        }
        return mapping;
    }

    private String extractTopicName(String className) {
        String[] parts = className.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        return parts[0] + "." + parts[1];
    }
}
