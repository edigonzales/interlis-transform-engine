package com.interlis.transform.interlis;

import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iom_j.itf.ItfWriter;
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
            reader = new XtfReader(path.toFile());
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
}
