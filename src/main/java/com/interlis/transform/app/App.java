package com.interlis.transform.app;

import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;
import com.interlis.transform.TransformationPlan;
import com.interlis.transform.TypeSystem;
import com.interlis.transform.engine.DefaultTransformationEngine;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class App {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Usage: interlis-transform-engine <mapping.yaml> <input.xtf|itf> <output.xtf|itf> [--modeldir <path>] <modelA> <modelB> [...]");
            return;
        }

        Path mappingPath = Path.of(args[0]);
        Path inputPath = Path.of(args[1]);
        Path outputPath = Path.of(args[2]);
        List<String> modelNames = new ArrayList<>();
        String modelDirectories = null;
        for (int i = 3; i < args.length; i++) {
            if ("--modeldir".equals(args[i]) && i + 1 < args.length) {
                modelDirectories = Path.of(args[i + 1]).toAbsolutePath().toString();
                i++;
                continue;
            }
            modelNames.add(args[i]);
        }
        if (modelNames.size() < 2) {
            throw new IllegalArgumentException("At least two model names are required (source and target).");
        }

        MappingConfig config = new MappingConfigLoader().load(mappingPath);
        InterlisModelLoader modelLoader = new InterlisModelLoader();
        TransferDescription sourceDescription = modelLoader.compileModel(modelNames.get(0), modelDirectories);
        TransferDescription targetDescription = modelLoader.compileModel(modelNames.get(1), modelDirectories);
        List<TransferDescription> allDescriptions = new ArrayList<>();
        allDescriptions.add(sourceDescription);
        allDescriptions.add(targetDescription);
        for (int i = 2; i < modelNames.size(); i++) {
            allDescriptions.add(modelLoader.compileModel(modelNames.get(i), modelDirectories));
        }
        TypeSystem typeSystem = new CompositeIliTypeSystem(allDescriptions);

        TransformationPlan plan = new MappingCompiler(typeSystem).compile(config);

        ExpressionEngine expressionEngine = new BasicExpressionEngine(new FunctionRegistry());
        DefaultTransformationEngine engine = new DefaultTransformationEngine(
                expressionEngine,
                new InMemoryStateStore(),
                Logger.getLogger(App.class.getName())
        );

        InterlisIoFactory ioFactory = new InterlisIoFactory();
        IoxReader reader = ioFactory.createReader(inputPath, sourceDescription);
        IoxWriter writer = ioFactory.createWriter(outputPath, targetDescription);
        try {
            engine.run(reader, writer, plan);
        } finally {
            reader.close();
        }
    }
}
