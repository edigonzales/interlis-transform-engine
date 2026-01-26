package com.interlis.transform;

public interface TypeSystem {
    boolean classExists(String className);

    boolean associationExists(String associationName);

    boolean roleExists(String viewableName, String roleName);

    boolean attributeExists(String className, String attributeName);

    boolean attributePathExists(String className, String attributePath);
}
