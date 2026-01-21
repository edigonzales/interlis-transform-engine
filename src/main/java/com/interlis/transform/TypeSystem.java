package com.interlis.transform;

public interface TypeSystem {
    boolean classExists(String className);

    boolean attributeExists(String className, String attributeName);
}
