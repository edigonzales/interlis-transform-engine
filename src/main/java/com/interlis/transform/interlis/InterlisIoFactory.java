package com.interlis.transform.interlis;

import ch.interlis.ili2c.metamodel.Model;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iom_j.itf.ItfWriter;
import ch.interlis.iom_j.xtf.Xtf23Reader;
import ch.interlis.iom_j.xtf.Xtf24Reader;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iom_j.xtf.XtfWriter;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;
import ch.interlis.iox_j.IoxIliReader;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public final class InterlisIoFactory {
    public InterlisFormat detectFormat(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xtf") || name.endsWith(".xtf.gz")) {
            return InterlisFormat.XTF;
        }
        if (name.endsWith(".itf") || name.endsWith(".itf.gz")) {
            return InterlisFormat.ITF;
        }
        throw new IllegalArgumentException("Unsupported INTERLIS transfer format for file: " + path);
    }

    public IoxReader createReader(Path path, TransferDescription transferDescription) throws Exception {
        Objects.requireNonNull(transferDescription, "transferDescription");
        InterlisFormat format = detectFormat(path);
        IoxReader reader;
        if (format == InterlisFormat.ITF) {
            reader = new ItfReader(path.toFile());
        } else {
            reader = createXtfReader(path, transferDescription);
        }
        if (reader instanceof IoxIliReader iliReader) {
            iliReader.setModel(transferDescription);
        }
        return reader;
    }

    public IoxWriter createWriter(Path path, TransferDescription transferDescription) throws Exception {
        Objects.requireNonNull(transferDescription, "transferDescription");
        InterlisFormat format = detectFormat(path);
        if (format == InterlisFormat.ITF) {
            return new ItfWriter(path.toFile(), transferDescription);
        }
        return new XtfWriter(path.toFile(), transferDescription);
    }

    private IoxReader createXtfReader(Path path, TransferDescription transferDescription) throws Exception {
        String iliVersion = detectIliVersion(transferDescription);
        if (Model.ILI2_4.equals(iliVersion)) {
            return new Xtf24Reader(path.toFile());
        }
        if (Model.ILI2_3.equals(iliVersion)) {
            return new Xtf23Reader(path.toFile());
        }
        return new XtfReader(path.toFile());
    }

    private String detectIliVersion(TransferDescription transferDescription) {
        Model[] models = transferDescription.getModelsFromLastFile();
        if (models != null && models.length > 0) {
            boolean saw23 = false;
            for (Model model : models) {
                String version = model.getIliVersion();
                if (Model.ILI2_4.equals(version)) {
                    return Model.ILI2_4;
                }
                if (Model.ILI2_3.equals(version)) {
                    saw23 = true;
                }
            }
            if (saw23) {
                return Model.ILI2_3;
            }
        }
        Model lastModel = transferDescription.getLastModel();
        if (lastModel != null) {
            return lastModel.getIliVersion();
        }
        return null;
    }
}
