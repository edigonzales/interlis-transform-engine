package com.interlis.transform;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.metamodel.TransferDescription;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InterlisModelLoader {
    public TransferDescription load(List<Path> modelFiles) throws Ili2cFailure {
        ArrayList<File> iliFiles = new ArrayList<>();
        Set<File> iliDirs = new HashSet<>();
        for (Path path : modelFiles) {
            File file = path.toFile();
            iliFiles.add(file);
            File parent = file.getParentFile();
            if (parent != null) {
                iliDirs.add(parent);
            }
        }
        return Ili2c.compileIliFiles(iliFiles, new ArrayList<>(iliDirs));
    }
}
