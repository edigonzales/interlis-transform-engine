package com.interlis.transform.processor;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import com.interlis.transform.TransformationContext;
import com.interlis.transform.engine.DefaultTransformationContext;
import com.interlis.transform.expression.BasicExpressionEngine;
import com.interlis.transform.expression.ExpressionEngine;
import com.interlis.transform.expression.FunctionRegistry;
import com.interlis.transform.state.InMemoryStateStore;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapAttributeProcessorTest {
    @Test
    void mapsGeometryObjectsFromExpression() {
        ExpressionEngine engine = new BasicExpressionEngine(new FunctionRegistry());
        Iom_jObject source = new Iom_jObject("ModelA.Foo", null);
        IomObject geometry = new Iom_jObject("ModelA.Geometry", null);
        source.addattrobj("geom", geometry);

        Iom_jObject target = new Iom_jObject("ModelB.Bar", null);
        target.addattrobj("geom", new Iom_jObject("ModelA.Geometry", null));

        TransformationContext context = new DefaultTransformationContext(
                source,
                emitted -> {
                },
                engine,
                new InMemoryStateStore(),
                Logger.getLogger("test")
        );
        context.target(target);

        MapAttributeProcessor processor = new MapAttributeProcessor("geom", "${src.geom}");
        processor.apply(context);

        assertThat(target.getattrvaluecount("geom")).isEqualTo(1);
        assertThat(target.getattrobj("geom", 0)).isSameAs(geometry);
    }
}
