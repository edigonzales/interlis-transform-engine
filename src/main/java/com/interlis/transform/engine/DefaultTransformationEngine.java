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
import com.interlis.transform.rules.TargetClassRuleSet;
import com.interlis.transform.state.StateStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        List<IoxEvent> events = new ArrayList<>();
        indexSourceObjects(reader, events);
        Map<String, String> topicMapping = buildTopicMapping(plan);
        boolean writeBasket = false;
        String currentBasketId = null;
        IoxEvent event;
        for (IoxEvent recorded : events) {
            event = recorded;
            if (event instanceof StartTransferEvent) {
                StartTransferEvent start = (StartTransferEvent) event;
                writer.write(new ch.interlis.iox_j.StartTransferEvent(start.getSender(), start.getComment(), start.getVersion()));
                continue;
            }
            if (event instanceof StartBasketEvent) {
                StartBasketEvent basket = (StartBasketEvent) event;
                currentBasketId = basket.getBid();
                String targetTopic = topicMapping.get(basket.getType());
                if (targetTopic != null) {
                    writer.write(new ch.interlis.iox_j.StartBasketEvent(targetTopic, resolveBasketId(plan, basket.getBid())));
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
                List<TargetClassRuleSet> rules = plan.rulesFor(source.getobjecttag());
                if (rules.isEmpty()) {
                    continue;
                }
                for (TargetClassRuleSet ruleSet : rules) {
                    applyRuleSet(ruleSet, source, currentBasketId, emitter);
                }
                continue;
            }
            if (event instanceof EndBasketEvent) {
                if (writeBasket) {
                    writer.write(new ch.interlis.iox_j.EndBasketEvent());
                }
                currentBasketId = null;
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

    private void indexSourceObjects(IoxReader reader, List<IoxEvent> events) throws Exception {
        IoxEvent event;
        String currentBasketId = null;
        while ((event = reader.read()) != null) {
            events.add(event);
            if (event instanceof StartBasketEvent) {
                StartBasketEvent basket = (StartBasketEvent) event;
                currentBasketId = basket.getBid();
                continue;
            }
            if (event instanceof EndBasketEvent) {
                currentBasketId = null;
                continue;
            }
            if (event instanceof ObjectEvent) {
                IomObject source = ((ObjectEvent) event).getIomObject();
                stateStore.indexObject(source.getobjecttag(), currentBasketId, source);
            }
        }
    }

    private void applyRuleSet(
            TargetClassRuleSet ruleSet,
            IomObject source,
            String currentBasketId,
            TargetEmitter emitter
    ) {
        Map<String, com.interlis.transform.mapping.SourceSpec> sourcesByAlias = new HashMap<>();
        for (com.interlis.transform.mapping.SourceSpec sourceSpec : ruleSet.sources()) {
            String alias = resolveAlias(sourceSpec);
            sourcesByAlias.put(alias, sourceSpec);
        }
        String primaryAlias = resolveAlias(ruleSet.primarySource());
        List<Map<String, IomObject>> contexts = new ArrayList<>();
        Map<String, IomObject> baseContext = new HashMap<>();
        baseContext.put(primaryAlias, source);
        contexts.add(baseContext);
        for (com.interlis.transform.mapping.JoinSpec joinSpec : ruleSet.joins()) {
            contexts = applyJoin(joinSpec, contexts, sourcesByAlias, primaryAlias, currentBasketId, emitter);
            if (contexts.isEmpty()) {
                return;
            }
        }
        for (Map<String, IomObject> contextSources : contexts) {
            DefaultTransformationContext context = new DefaultTransformationContext(
                    contextSources,
                    primaryAlias,
                    emitter,
                    expressionEngine,
                    stateStore,
                    logger
            );
            List<Processor> processors = ruleSet.processors();
            for (Processor processor : processors) {
                processor.apply(context);
                if (context.shouldSkip()) {
                    break;
                }
            }
        }
    }

    private List<Map<String, IomObject>> applyJoin(
            com.interlis.transform.mapping.JoinSpec joinSpec,
            List<Map<String, IomObject>> contexts,
            Map<String, com.interlis.transform.mapping.SourceSpec> sourcesByAlias,
            String primaryAlias,
            String currentBasketId,
            TargetEmitter emitter
    ) {
        com.interlis.transform.mapping.SourceSpec rightSource = sourcesByAlias.get(joinSpec.getRightAlias());
        if (rightSource == null) {
            return List.of();
        }
        List<IomObject> candidates = stateStore.listObjects(rightSource.getSourceClass(), currentBasketId);
        List<Map<String, IomObject>> nextContexts = new ArrayList<>();
        for (Map<String, IomObject> context : contexts) {
            IomObject left = context.get(joinSpec.getLeftAlias());
            if (left == null) {
                continue;
            }
            for (IomObject candidate : candidates) {
                Map<String, IomObject> expanded = new HashMap<>(context);
                expanded.put(joinSpec.getRightAlias(), candidate);
                if (joinSpec.getExpr() != null && !joinSpec.getExpr().isBlank()) {
                    DefaultTransformationContext joinContext = new DefaultTransformationContext(
                            expanded,
                            primaryAlias,
                            emitter,
                            expressionEngine,
                            stateStore,
                            logger
                    );
                    Object result = expressionEngine.evaluate(joinSpec.getExpr(), joinContext);
                    boolean matches = result instanceof Boolean
                            ? (Boolean) result
                            : result != null && Boolean.parseBoolean(result.toString());
                    if (!matches) {
                        continue;
                    }
                }
                nextContexts.add(expanded);
            }
        }
        return nextContexts;
    }

    private String resolveAlias(com.interlis.transform.mapping.SourceSpec sourceSpec) {
        if (sourceSpec.getAlias() == null || sourceSpec.getAlias().isBlank()) {
            return "src";
        }
        return sourceSpec.getAlias();
    }

    private Map<String, String> buildTopicMapping(TransformationPlan plan) {
        Map<String, String> mapping = new HashMap<>();
        for (String sourceClass : plan.sourceClassNames()) {
            List<TargetClassRuleSet> rules = plan.rulesFor(sourceClass);
            if (rules.isEmpty()) {
                continue;
            }
            String sourceTopic = extractTopicName(sourceClass);
            if (sourceTopic == null) {
                continue;
            }
            for (TargetClassRuleSet ruleSet : rules) {
                String targetClass = ruleSet.targetClassName();
                if (targetClass == null) {
                    continue;
                }
                String targetTopic = extractTopicName(targetClass);
                if (targetTopic != null) {
                    mapping.putIfAbsent(sourceTopic, targetTopic);
                }
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

    private String resolveBasketId(TransformationPlan plan, String sourceBid) {
        Optional<String> basketIdStrategy = plan.basketIdStrategy();
        if (basketIdStrategy.isEmpty() || "preserve".equalsIgnoreCase(basketIdStrategy.get())) {
            return sourceBid;
        }
        if ("uuid".equalsIgnoreCase(basketIdStrategy.get()) || "generate".equalsIgnoreCase(basketIdStrategy.get())) {
            return UUID.randomUUID().toString();
        }
        if ("integer".equalsIgnoreCase(basketIdStrategy.get())) {
            return Long.toString(stateStore.nextBasketId());
        }
        return sourceBid;
    }
}
