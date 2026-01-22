package com.interlis.transform.emit;

import ch.interlis.iom.IomObject;
import ch.interlis.iox.IoxWriter;
import ch.interlis.iox_j.ObjectEvent;
import java.util.Objects;

public final class WriterTargetEmitter implements TargetEmitter {
    private final IoxWriter writer;

    public WriterTargetEmitter(IoxWriter writer) {
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    @Override
    public void emit(IomObject target) {
        try {
            writer.write(new ObjectEvent(target));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write target object", e);
        }
    }
}
