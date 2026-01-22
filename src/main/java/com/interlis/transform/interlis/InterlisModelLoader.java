package com.interlis.transform.interlis;

import ch.ehi.basics.logging.EhiLogger;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.TransferDescription;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class InterlisModelLoader {
    public TransferDescription compileModel(String modelName, String modelDirectories) throws Ili2cFailure {
        EhiLogger.logState("ili2c-" + TransferDescription.getVersion());

        Ili2cSettings settings = new Ili2cSettings();
        ch.interlis.ili2c.Main.setDefaultIli2cPathMap(settings);
        if (modelDirectories != null && !modelDirectories.isBlank()) {
            settings.setIlidirs(modelDirectories);
        } else {
            settings.setIlidirs(Ili2cSettings.DEFAULT_ILIDIRS);
        }

        Configuration cfg = new Configuration();
        cfg.addFileEntry(new FileEntry(modelName, FileEntryKind.ILIMODELFILE));
        cfg.setAutoCompleteModelList(true);
        cfg.setGenerateWarnings(true);

        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateOut = dateFormatter.format(new Date());

        TransferDescription td = ch.interlis.ili2c.Main.runCompiler(cfg, settings, null);

        if (td == null) {
            EhiLogger.logError("...compiler run failed " + dateOut);
            throw new IllegalStateException("ili2c compiler run failed for model: " + modelName);
        }

        EhiLogger.logState("...compiler run done " + dateOut);
        return td;
    }
}
