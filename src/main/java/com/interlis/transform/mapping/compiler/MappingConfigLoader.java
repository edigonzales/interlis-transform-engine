package com.interlis.transform.mapping.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.interlis.transform.mapping.MappingConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MappingConfigLoader {
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public MappingConfig load(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return mapper.readValue(inputStream, MappingConfig.class);
        }
    }
}
