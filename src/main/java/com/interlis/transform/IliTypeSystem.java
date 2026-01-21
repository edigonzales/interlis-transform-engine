package com.interlis.transform;

import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.TransferDescription;
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
    public boolean attributeExists(String className, String attributeName) {
        Element element = transferDescription.getElement(className);
        if (!(element instanceof Viewable)) {
            return false;
        }
        Viewable<?> viewable = (Viewable<?>) element;
        return viewable.findAttribute(attributeName) != null;
    }
}
