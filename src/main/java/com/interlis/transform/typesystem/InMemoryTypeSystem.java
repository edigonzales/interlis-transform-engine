package com.interlis.transform.typesystem;

import com.interlis.transform.TypeSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InMemoryTypeSystem implements TypeSystem {
    private final Map<String, Set<String>> classes = new HashMap<>();
    private final Set<String> associations = new HashSet<>();
    private final Map<String, Set<String>> roles = new HashMap<>();

    public InMemoryTypeSystem registerClass(String className, Set<String> attributes) {
        classes.put(className, new HashSet<>(attributes));
        return this;
    }

    public InMemoryTypeSystem registerAssociation(String associationName) {
        associations.add(associationName);
        return this;
    }

    public InMemoryTypeSystem registerRoles(String viewableName, Set<String> roleNames) {
        roles.put(viewableName, new HashSet<>(roleNames));
        return this;
    }

    @Override
    public boolean classExists(String className) {
        return classes.containsKey(className);
    }

    @Override
    public boolean associationExists(String associationName) {
        return associations.contains(associationName);
    }

    @Override
    public boolean roleExists(String viewableName, String roleName) {
        Set<String> roleSet = roles.get(viewableName);
        return roleSet != null && roleSet.contains(roleName);
    }

    @Override
    public boolean attributeExists(String className, String attributeName) {
        return attributePathExists(className, attributeName);
    }

    @Override
    public boolean attributePathExists(String className, String attributePath) {
        Set<String> attrs = classes.get(className);
        return attrs != null && attrs.contains(attributePath);
    }
}
