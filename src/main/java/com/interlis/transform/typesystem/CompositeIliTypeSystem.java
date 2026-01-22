package com.interlis.transform.typesystem;

import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.TransferDescription;
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
    public boolean attributeExists(String className, String attributeName) {
        for (TransferDescription td : transferDescriptions) {
            Element element = td.getElement(className);
            if (element instanceof Viewable) {
                Viewable<?> viewable = (Viewable<?>) element;
                if (viewable.findAttribute(attributeName) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isViewable(Element element) {
        return element instanceof Viewable;
    }
}
