package com.interlis.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MappingConfigLoaderTest {
    @Test
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
        MappingRule rule = config.getMappings().get(0);
        assertThat(rule.getSourceClass()).isEqualTo("DM01AVCH24LV95D.Liegenschaften.LSNachfuehrung");
        assertThat(rule.getTargetClass()).isEqualTo("DMAV_Grundstuecke_V1_0.Grundstuecke.GSNachfuehrung");
        assertThat(rule.getAttributes()).hasSize(7);
    }
}
