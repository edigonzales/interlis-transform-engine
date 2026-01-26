package com.interlis.transform.typesystem;

import ch.interlis.ili2c.metamodel.AssociationDef;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CompositionType;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.ObjectType;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.ili2c.metamodel.Viewable;
import com.interlis.transform.TypeSystem;
import java.util.List;
import java.util.Objects;

public final class CompositeIliTypeSystem implements TypeSystem {
    private final List<TransferDescription> transferDescriptions;

    public CompositeIliTypeSystem(List<TransferDescription> transferDescriptions) {
        this.transferDescriptions = List.copyOf(Objects.requireNonNull(transferDescriptions, "transferDescriptions"));
    }

    @Override
    public boolean classExists(String className) {
        return transferDescriptions.stream().anyMatch(td -> isViewable(td.getElement(className)));
    }

    @Override
    public boolean associationExists(String associationName) {
        return transferDescriptions.stream().anyMatch(td -> td.getElement(associationName) instanceof AssociationDef);
    }

    @Override
    public boolean roleExists(String viewableName, String roleName) {
        if (viewableName == null || roleName == null) {
            return false;
        }
        for (TransferDescription td : transferDescriptions) {
            Element element = td.getElement(viewableName);
            if (element instanceof Viewable) {
                Viewable<?> viewable = (Viewable<?>) element;
                if (viewable.findRole(roleName) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean attributeExists(String className, String attributeName) {
        return attributePathExists(className, attributeName);
    }

    @Override
    public boolean attributePathExists(String className, String attributePath) {
        for (TransferDescription td : transferDescriptions) {
            Element element = td.getElement(className);
            if (element instanceof Viewable) {
                Viewable<?> viewable = (Viewable<?>) element;
                if (attributePathExists(viewable, attributePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isViewable(Element element) {
        return element instanceof Viewable;
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
