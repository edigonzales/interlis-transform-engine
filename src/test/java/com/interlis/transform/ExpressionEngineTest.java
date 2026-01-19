package com.interlis.transform;

import ch.interlis.iom_j.Iom_jObject;
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
}
