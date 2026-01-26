package com.interlis.transform.engine;

import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;
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
        Path inputPath = Path.of("src/test/data/DMAV_Grundstuecke_V1_0.449.xtf");
        Path expectedPath = Path.of("src/test/data/DM01_Grundstuecke_449.itf");
        Path outputPath = tempDir.resolve("DM01_Grundstuecke_449.itf");

        MappingConfig config = new MappingConfigLoader().load(mappingPath);

        String modelDirectories = modelDirectories();
        InterlisModelLoader modelLoader = new InterlisModelLoader();
        TransferDescription sourceDescription = modelLoader.compileModel("DMAV_Grundstuecke_V1_0", modelDirectories);
        TransferDescription targetDescription = modelLoader.compileModel("DM01AVCH24LV95D", modelDirectories);
        TypeSystem typeSystem = new CompositeIliTypeSystem(List.of(sourceDescription, targetDescription));

        TransformationPlan plan = new MappingCompiler(typeSystem).compile(config);

        ExpressionEngine expressionEngine = new BasicExpressionEngine(new FunctionRegistry());
        DefaultTransformationEngine engine = new DefaultTransformationEngine(
                expressionEngine,
                new InMemoryStateStore(),
                Logger.getLogger(DmavToDm01IntegrationIT.class.getName())
        );

        InterlisIoFactory ioFactory = new InterlisIoFactory();
        IoxReader reader = ioFactory.createReader(inputPath, sourceDescription);
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
}
