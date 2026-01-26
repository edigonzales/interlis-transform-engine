package com.interlis.transform.mapping.compiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.interlis.transform.mapping.MappingConfig;
import com.interlis.transform.mapping.SourceSpec;
import com.interlis.transform.mapping.TargetMapping;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MappingConfigLoader {
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public MappingConfig load(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            JsonNode root = mapper.readTree(inputStream);
            MappingConfig config = new MappingConfig();
            config.setBasketIdStrategy(textValue(root, "basketIdStrategy"));
            List<TargetMapping> mappings = readMappings(root.get("mappings"));
            config.setMappings(mappings);
            return config;
        }
    }

    private List<TargetMapping> readMappings(JsonNode mappingsNode) {
        if (mappingsNode == null || !mappingsNode.isArray()) {
            return null;
        }
        List<TargetMapping> mappings = new ArrayList<>();
        for (JsonNode mappingNode : mappingsNode) {
            if (isLegacyMapping(mappingNode)) {
                mappings.add(convertLegacy(mappingNode));
            } else {
                mappings.add(mapper.convertValue(mappingNode, TargetMapping.class));
            }
        }
        return mappings;
    }

    private boolean isLegacyMapping(JsonNode mappingNode) {
        return mappingNode.hasNonNull("sourceClass") && mappingNode.hasNonNull("targetClass");
    }

    private TargetMapping convertLegacy(JsonNode mappingNode) {
        LegacyMappingRule legacy = mapper.convertValue(mappingNode, LegacyMappingRule.class);
        TargetMapping mapping = new TargetMapping();
        mapping.setTargetClass(legacy.getTargetClass());
        mapping.setOidStrategy(legacy.getOidStrategy());
        mapping.setAttributes(legacy.getAttributes());
        mapping.setFilters(legacy.getFilters());
        SourceSpec source = new SourceSpec();
        source.setSourceClass(legacy.getSourceClass());
        mapping.setSources(List.of(source));
        return mapping;
    }

    private String textValue(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return null;
        }
        JsonNode value = node.get(field);
        return value.isNull() ? null : value.asText(null);
    }

    private static final class LegacyMappingRule {
        private String sourceClass;
        private String targetClass;
        private String oidStrategy;
        private List<com.interlis.transform.mapping.AttributeMapping> attributes;
        private List<com.interlis.transform.mapping.FilterRule> filters;

        public String getSourceClass() {
            return sourceClass;
        }

        public void setSourceClass(String sourceClass) {
            this.sourceClass = sourceClass;
        }

        public String getTargetClass() {
            return targetClass;
        }

        public void setTargetClass(String targetClass) {
            this.targetClass = targetClass;
        }

        public String getOidStrategy() {
            return oidStrategy;
        }

        public void setOidStrategy(String oidStrategy) {
            this.oidStrategy = oidStrategy;
        }

        public List<com.interlis.transform.mapping.AttributeMapping> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<com.interlis.transform.mapping.AttributeMapping> attributes) {
            this.attributes = attributes;
        }

        public List<com.interlis.transform.mapping.FilterRule> getFilters() {
            return filters;
        }

        public void setFilters(List<com.interlis.transform.mapping.FilterRule> filters) {
            this.filters = filters;
        }
    }
}
