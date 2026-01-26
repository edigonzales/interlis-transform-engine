package com.interlis.transform.engine;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxFactoryCollection;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;
import ch.interlis.iox_j.EndBasketEvent;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.StartBasketEvent;
import ch.interlis.iox_j.StartTransferEvent;
import com.interlis.transform.TransformationPlan;
import com.interlis.transform.TypeSystem;
import com.interlis.transform.expression.BasicExpressionEngine;
import com.interlis.transform.expression.ExpressionEngine;
import com.interlis.transform.expression.FunctionRegistry;
import com.interlis.transform.mapping.AttributeMapping;
import com.interlis.transform.mapping.MappingConfig;
import com.interlis.transform.mapping.SourceSpec;
import com.interlis.transform.mapping.TargetMapping;
import com.interlis.transform.mapping.compiler.MappingCompiler;
import com.interlis.transform.state.InMemoryStateStore;
import com.interlis.transform.typesystem.InMemoryTypeSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransformationEngineIT {
    @Test
    void runsEndToEndMapping() throws Exception {
        MappingConfig config = new MappingConfig();
        TargetMapping rule = new TargetMapping();
        SourceSpec source = new SourceSpec();
        source.setSourceClass("ModelA.Foo");
        rule.setSources(List.of(source));
        rule.setTargetClass("ModelB.Bar");
        AttributeMapping attrY = new AttributeMapping();
        attrY.setTarget("y");
        attrY.setExpr("${src.x}");
        AttributeMapping attrTotal = new AttributeMapping();
        attrTotal.setTarget("total");
        attrTotal.setExpr("${src.a} + ${src.b}");
        rule.setAttributes(List.of(attrY, attrTotal));
        config.setMappings(List.of(rule));

        TypeSystem typeSystem = new InMemoryTypeSystem()
                .registerClass("ModelA.Foo", Set.of("x", "a", "b"))
                .registerClass("ModelB.Bar", Set.of("y", "total"));
        TransformationPlan plan = new MappingCompiler(typeSystem).compile(config);

        ExpressionEngine expressionEngine = new BasicExpressionEngine(new FunctionRegistry());
        DefaultTransformationEngine engine = new DefaultTransformationEngine(
                expressionEngine,
                new InMemoryStateStore(),
                Logger.getLogger("test"),
                com.interlis.transform.RoleResolver.NONE
        );

        Iom_jObject source = new Iom_jObject("ModelA.Foo", null);
        source.setattrvalue("x", "42");
        source.setattrvalue("a", "1");
        source.setattrvalue("b", "2");

        CollectingWriter writer = new CollectingWriter();
        engine.run(new SingleObjectReader(source), writer, plan);

        assertThat(writer.objects()).hasSize(1);
        IomObject target = writer.objects().get(0);
        assertThat(target.getobjecttag()).isEqualTo("ModelB.Bar");
        assertThat(target.getattrvalue("y")).isEqualTo("42");
        assertThat(target.getattrvalue("total")).isEqualTo("3");
    }

    private static final class SingleObjectReader implements IoxReader {
        private final List<IoxEvent> events;
        private int index;
        private IoxFactoryCollection factory;

        private SingleObjectReader(IomObject object) {
            this.events = List.of(
                    new StartTransferEvent("test-sender", "test-comment", "1.0"),
                    new StartBasketEvent("ModelA.Foo", "1"),
                    new ObjectEvent(object),
                    new EndBasketEvent(),
                    new EndTransferEvent()
            );
        }

        @Override
        public IoxEvent read() {
            if (index >= events.size()) {
                return null;
            }
            return events.get(index++);
        }

        @Override
        public void close() {
            // no-op
        }

        @Override
        public void setFactory(IoxFactoryCollection factory) {
            this.factory = factory;
        }

        @Override
        public IoxFactoryCollection getFactory() {
            return factory;
        }

        @Override
        public IomObject createIomObject(String type, String oid) {
            return new Iom_jObject(type, oid);
        }
    }

    private static final class CollectingWriter implements IoxWriter {
        private final List<IomObject> objects = new ArrayList<>();
        private IoxFactoryCollection factory;

        @Override
        public void write(IoxEvent event) {
            if (event instanceof ObjectEvent) {
                objects.add(((ObjectEvent) event).getIomObject());
            }
        }

        @Override
        public void close() {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void setFactory(IoxFactoryCollection factory) {
            this.factory = factory;
        }

        @Override
        public IoxFactoryCollection getFactory() {
            return factory;
        }

        @Override
        public IomObject createIomObject(String type, String oid) {
            return new Iom_jObject(type, oid);
        }

        public List<IomObject> objects() {
            return objects;
        }
    }
}
