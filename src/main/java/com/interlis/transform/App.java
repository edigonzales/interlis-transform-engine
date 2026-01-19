package com.interlis.transform;

import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public final class App {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: interlis-transform-engine <mapping.yaml> <input.xtf> <output.xtf> <modelA.ili> <modelB.ili> [...]");
            return;
        }

        Path mappingPath = Path.of(args[0]);
        Path inputPath = Path.of(args[1]);
        Path outputPath = Path.of(args[2]);
        List<Path> modelFiles = Arrays.stream(args).skip(3).map(Path::of).toList();

        MappingConfig config = new MappingConfigLoader().load(mappingPath);
        TransferDescription transferDescription = new InterlisModelLoader().load(modelFiles);
        TypeSystem typeSystem = new IliTypeSystem(transferDescription);

        TransformationPlan plan = new MappingCompiler(typeSystem).compile(config);

        ExpressionEngine expressionEngine = new BasicExpressionEngine(new FunctionRegistry());
        DefaultTransformationEngine engine = new DefaultTransformationEngine(
                expressionEngine,
                new InMemoryStateStore(),
                Logger.getLogger(App.class.getName())
        );

        InterlisIoFactory ioFactory = new InterlisIoFactory();
        IoxReader reader = ioFactory.createReader(inputPath, transferDescription);
        IoxWriter writer = ioFactory.createWriter(outputPath, transferDescription);
        try {
            engine.run(reader, writer, plan);
        } finally {
            reader.close();
        }
    }
}
