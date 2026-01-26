package com.interlis.transform.engine;

import ch.interlis.ili2c.metamodel.TransferDescription;
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
import com.interlis.transform.RoleResolver;
import com.interlis.transform.TransformationPlan;
import com.interlis.transform.TypeSystem;
import com.interlis.transform.expression.BasicExpressionEngine;
import com.interlis.transform.expression.ExpressionEngine;
import com.interlis.transform.expression.FunctionRegistry;
import com.interlis.transform.interlis.InterlisIoFactory;
import com.interlis.transform.interlis.InterlisModelLoader;
import com.interlis.transform.mapping.MappingConfig;
import com.interlis.transform.mapping.compiler.MappingCompiler;
import com.interlis.transform.mapping.compiler.MappingConfigLoader;
import com.interlis.transform.state.InMemoryStateStore;
import com.interlis.transform.typesystem.CompositeIliRoleResolver;
import com.interlis.transform.typesystem.CompositeIliTypeSystem;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DmavToDm01IntegrationIT {
    @Test
    void transformsDmavXtfToDm01Itf(@TempDir Path tempDir) throws Exception {
        Path mappingPath = resourcePath("mappings/dmav_to_dm01.yaml");
        Path expectedPath = Path.of("src/test/data/DM01_Grundstuecke_449.itf");
        Path outputPath = tempDir.resolve("DM01_Grundstuecke_449.itf");

        MappingConfig config = new MappingConfigLoader().load(mappingPath);

        String modelDirectories = modelDirectories();
        InterlisModelLoader modelLoader = new InterlisModelLoader();
        TransferDescription sourceDescription = modelLoader.compileModel("DMAV_Grundstuecke_V1_0", modelDirectories);
        TransferDescription targetDescription = modelLoader.compileModel("DM01AVCH24LV95D", modelDirectories);
        TypeSystem typeSystem = new CompositeIliTypeSystem(List.of(sourceDescription, targetDescription));
        RoleResolver roleResolver = new CompositeIliRoleResolver(List.of(sourceDescription, targetDescription));

        TransformationPlan plan = new MappingCompiler(typeSystem).compile(config);

        ExpressionEngine expressionEngine = new BasicExpressionEngine(new FunctionRegistry());
        DefaultTransformationEngine engine = new DefaultTransformationEngine(
                expressionEngine,
                new InMemoryStateStore(),
                Logger.getLogger(DmavToDm01IntegrationIT.class.getName()),
                roleResolver
        );

        InterlisIoFactory ioFactory = new InterlisIoFactory();
        IoxReader reader = new SingleObjectReader(buildSourceObject());
        try {
            IoxWriter writer = ioFactory.createWriter(outputPath, targetDescription);
            engine.run(reader, writer, plan);
        } finally {
            reader.close();
        }

        String actual = normalizeLineEndings(Files.readString(outputPath, StandardCharsets.UTF_8));
        String expected = normalizeLineEndings(Files.readString(expectedPath, StandardCharsets.UTF_8));
        assertThat(actual).isEqualTo(expected);
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static Path resourcePath(String resource) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(
                DmavToDm01IntegrationIT.class.getClassLoader().getResource(resource),
                "Missing resource: " + resource
        ).toURI());
    }

    private static String modelDirectories() throws URISyntaxException {
        Path repositoryRoot = resourcePath("models/ilimodels.xml").getParent();
        Path coreModels = resourcePath("models/core/Units.ili").getParent();
        return String.join(";", repositoryRoot.toAbsolutePath().toString(), coreModels.toAbsolutePath().toString());
    }

    private static Iom_jObject buildSourceObject() {
        Iom_jObject source = new Iom_jObject(
                "DMAV_Grundstuecke_V1_0.Grundstuecke.GSNachfuehrung",
                "84c6bf54-18a5-43db-bfc0-67d96ff58431"
        );
        source.setattrvalue("NBIdent", "BE0200000428");
        source.setattrvalue("Identifikator", "202200001");
        source.setattrvalue("Beschreibung", "Mutation standard");
        source.addattrobj("Perimeter", buildPerimeter());
        source.setattrvalue("Mutationsart", "Normal");
        source.setattrvalue("GueltigerEintrag", "2022-04-01T12:00:00");
        source.setattrvalue("Grundbucheintrag", "2023-01-13T12:00:00");
        return source;
    }

    private static Iom_jObject buildPerimeter() {
        Iom_jObject surface = new Iom_jObject("SURFACE", null);
        IomObject boundary = surface.addattrobj("boundary", "BOUNDARY");
        IomObject polyline = boundary.addattrobj("polyline", "POLYLINE");
        IomObject segments = polyline.addattrobj("sequence", "SEGMENTS");
        addCoord(segments, "2586206.646", "1224211.203");
        addCoord(segments, "2586212.793", "1224233.214");
        addCoord(segments, "2586233.27", "1224226.726");
        addCoord(segments, "2586241.246", "1224224.391");
        addCoord(segments, "2586260.536", "1224219.398");
        addCoord(segments, "2586253.466", "1224197.011");
        addCoord(segments, "2586226.795", "1224205.078");
        addCoord(segments, "2586206.646", "1224211.203");
        return surface;
    }

    private static void addCoord(IomObject segments, String c1, String c2) {
        IomObject coord = segments.addattrobj("segment", "COORD");
        coord.setattrvalue("C1", c1);
        coord.setattrvalue("C2", c2);
    }

    private static final class SingleObjectReader implements IoxReader {
        private final List<IoxEvent> events;
        private int index;
        private IoxFactoryCollection factory;

        private SingleObjectReader(IomObject object) {
            this.events = List.of(
                    new StartTransferEvent("test-sender", "", "1.0"),
                    new StartBasketEvent(
                            "DMAV_Grundstuecke_V1_0.Grundstuecke",
                            "2dd5ce7d-c5fe-48ca-8748-41c84ae51b4b"
                    ),
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
}
