package com.interlis.transform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InMemoryTypeSystem implements TypeSystem {
    private final Map<String, Set<String>> classes = new HashMap<>();

    public InMemoryTypeSystem registerClass(String className, Set<String> attributes) {
        classes.put(className, new HashSet<>(attributes));
        return this;
    }

    @Override
    public boolean classExists(String className) {
        return classes.containsKey(className);
    }

    @Override
    public boolean attributeExists(String className, String attributeName) {
        Set<String> attrs = classes.get(className);
        return attrs != null && attrs.contains(attributeName);
    }
}
