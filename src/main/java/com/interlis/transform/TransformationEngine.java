package com.interlis.transform;

import ch.interlis.iox.IoxReader;
import ch.interlis.iox.IoxWriter;

public interface TransformationEngine {
    void run(IoxReader reader, IoxWriter writer, TransformationPlan plan) throws Exception;
}
