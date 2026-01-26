package com.interlis.transform.mapping.compiler;

import com.interlis.transform.mapping.MappingConfig;
import com.interlis.transform.mapping.SourceSpec;
import com.interlis.transform.mapping.TargetMapping;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MappingConfigLoaderTest {
    @Test
    @Disabled("Requires missing mappings/liegenschaften-to-grundstuecke.yaml resource")
    void loadsSampleMappingFromResources() throws IOException {
        Path tempFile = Files.createTempFile("mapping", ".yaml");
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("mappings/liegenschaften-to-grundstuecke.yaml")) {
            assertThat(inputStream).isNotNull();
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        MappingConfig config = new MappingConfigLoader().load(tempFile);

        assertThat(config.getMappings()).isNotNull();
        assertThat(config.getMappings()).hasSize(1);
        TargetMapping rule = config.getMappings().get(0);
        assertThat(rule.getTargetClass()).isEqualTo("DMAV_Grundstuecke_V1_0.Grundstuecke.GSNachfuehrung");
        assertThat(rule.getAttributes()).hasSize(7);
        SourceSpec source = rule.getSources().get(0);
        assertThat(source.getSourceClass()).isEqualTo("DM01AVSO24LV95.Liegenschaften.LSNachfuehrung");
    }
}
