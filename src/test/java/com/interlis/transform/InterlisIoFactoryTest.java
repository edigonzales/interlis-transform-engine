package com.interlis.transform;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterlisIoFactoryTest {
    private final InterlisIoFactory factory = new InterlisIoFactory();

    @Test
    void detectsXtfFormat() {
        assertThat(factory.detectFormat(Path.of("data.xtf"))).isEqualTo(InterlisFormat.XTF);
        assertThat(factory.detectFormat(Path.of("data.XTF.GZ"))).isEqualTo(InterlisFormat.XTF);
    }

    @Test
    void detectsItfFormat() {
        assertThat(factory.detectFormat(Path.of("data.itf"))).isEqualTo(InterlisFormat.ITF);
        assertThat(factory.detectFormat(Path.of("data.ITF.GZ"))).isEqualTo(InterlisFormat.ITF);
    }

    @Test
    void rejectsUnknownFormat() {
        assertThatThrownBy(() -> factory.detectFormat(Path.of("data.xml")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported INTERLIS transfer format");
    }
}
