package com.interlis.transform;

import ch.interlis.ili2c.metamodel.AssociationDef;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CompositionType;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.ObjectType;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.ili2c.metamodel.Viewable;
import java.util.Objects;

public final class IliTypeSystem implements TypeSystem {
    private final TransferDescription transferDescription;

    public IliTypeSystem(TransferDescription transferDescription) {
        this.transferDescription = Objects.requireNonNull(transferDescription, "transferDescription");
    }

    @Override
    public boolean classExists(String className) {
        Element element = transferDescription.getElement(className);
        return element instanceof Viewable;
    }

    @Override
    public boolean associationExists(String associationName) {
        Element element = transferDescription.getElement(associationName);
        return element instanceof AssociationDef;
    }

    @Override
    public boolean roleExists(String viewableName, String roleName) {
        if (viewableName == null || roleName == null) {
            return false;
        }
        Element element = transferDescription.getElement(viewableName);
        if (!(element instanceof Viewable)) {
            return false;
        }
        Viewable<?> viewable = (Viewable<?>) element;
        return viewable.findRole(roleName) != null;
    }

    @Override
    public boolean attributeExists(String className, String attributeName) {
        return attributePathExists(className, attributeName);
    }

    @Override
    public boolean attributePathExists(String className, String attributePath) {
        Element element = transferDescription.getElement(className);
        if (!(element instanceof Viewable)) {
            return false;
        }
        Viewable<?> viewable = (Viewable<?>) element;
        return attributePathExists(viewable, attributePath);
    }

    private boolean attributePathExists(Viewable<?> viewable, String attributePath) {
        if (attributePath == null || attributePath.isBlank()) {
            return false;
        }
        String[] segments = attributePath.split("\\.");
        Viewable<?> current = viewable;
        AttributeDef currentAttribute = null;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isBlank()) {
                return false;
            }
            currentAttribute = current.findAttribute(segment);
            if (currentAttribute == null) {
                return false;
            }
            if (i < segments.length - 1) {
                Viewable<?> next = resolveStructuredViewable(currentAttribute);
                if (next == null) {
                    return false;
                }
                current = next;
            }
        }
        return currentAttribute != null;
    }

    private Viewable<?> resolveStructuredViewable(AttributeDef attributeDef) {
        Type realType = Type.findReal(attributeDef.getDomain());
        if (realType instanceof CompositionType) {
            return ((CompositionType) realType).getComponentType();
        }
        if (realType instanceof ObjectType) {
            return ((ObjectType) realType).getRef();
        }
        return null;
    }
}
