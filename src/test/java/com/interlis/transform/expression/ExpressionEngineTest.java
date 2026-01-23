package com.interlis.transform.expression;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import com.interlis.transform.TransformationContext;
import com.interlis.transform.engine.DefaultTransformationContext;
import com.interlis.transform.state.InMemoryStateStore;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionEngineTest {
    @Test
    void evaluatesSubstringAndCoalesce() {
        ExpressionEngine engine = new BasicExpressionEngine(new FunctionRegistry());
        Iom_jObject source = new Iom_jObject("ModelA.Foo", null);
        source.setattrvalue("name", "Interlis");

        TransformationContext context = new DefaultTransformationContext(
                source,
                target -> {
                },
                engine,
                new InMemoryStateStore(),
                Logger.getLogger("test")
        );

        Object result = engine.evaluate("substring(${src.name}, 0, 5)", context);
        Object coalesced = engine.evaluate("coalesce('', ${src.name})", context);

        assertThat(result).isEqualTo("Inter");
        assertThat(coalesced).isEqualTo("Interlis");
    }

    @Test
    void evaluatesAdditionAndComparison() {
        ExpressionEngine engine = new BasicExpressionEngine(new FunctionRegistry());
        Iom_jObject source = new Iom_jObject("ModelA.Foo", null);
        source.setattrvalue("a", "3");
        source.setattrvalue("b", "4");

        TransformationContext context = new DefaultTransformationContext(
                source,
                target -> {
                },
                engine,
                new InMemoryStateStore(),
                Logger.getLogger("test")
        );

        Object sum = engine.evaluate("${src.a} + ${src.b}", context);
        Object comparison = engine.evaluate("${src.a} != '5'", context);

        assertThat(sum).isEqualTo("7");
        assertThat(comparison).isEqualTo(true);
    }

    @Test
    void evaluatesToXmlDate() {
        ExpressionEngine engine = new BasicExpressionEngine(new FunctionRegistry());
        Iom_jObject source = new Iom_jObject("ModelA.Foo", null);
        TransformationContext context = new DefaultTransformationContext(
                source,
                target -> {
                },
                engine,
                new InMemoryStateStore(),
                Logger.getLogger("test")
        );

        Object formatted = engine.evaluate("to_xml_date('20010302', 'yyyyMMdd')", context);
        Object emptyValue = engine.evaluate("to_xml_date('', 'yyyyMMdd')", context);

        assertThat(formatted).isEqualTo("2001-03-02");
        assertThat(emptyValue).isNull();
    }

    @Test
    void evaluatesToXmlDateTime() {
        ExpressionEngine engine = new BasicExpressionEngine(new FunctionRegistry());
        Iom_jObject source = new Iom_jObject("ModelA.Foo", null);
        TransformationContext context = new DefaultTransformationContext(
                source,
                target -> {
                },
                engine,
                new InMemoryStateStore(),
                Logger.getLogger("test")
        );

        Object formattedDate = engine.evaluate("to_xml_datetime('20010302', 'yyyyMMdd')", context);
        Object formattedDateTime = engine.evaluate("to_xml_datetime('2001-03-02 12:30:15', 'yyyy-MM-dd HH:mm:ss')", context);
        Object emptyValue = engine.evaluate("to_xml_datetime('', 'yyyyMMdd')", context);

        assertThat(formattedDate).isEqualTo("2001-03-02T00:00:00");
        assertThat(formattedDateTime).isEqualTo("2001-03-02T12:30:15");
        assertThat(emptyValue).isNull();
    }

    @Test
    void resolvesSourceAttributeObjects() {
        ExpressionEngine engine = new BasicExpressionEngine(new FunctionRegistry());
        Iom_jObject source = new Iom_jObject("ModelA.Foo", null);
        IomObject geometry = new Iom_jObject("ModelA.Geometry", null);
        source.addattrobj("geom", geometry);

        TransformationContext context = new DefaultTransformationContext(
                source,
                target -> {
                },
                engine,
                new InMemoryStateStore(),
                Logger.getLogger("test")
        );

        Object result = engine.evaluate("${src.geom}", context);

        assertThat(result).isSameAs(geometry);
    }
}
