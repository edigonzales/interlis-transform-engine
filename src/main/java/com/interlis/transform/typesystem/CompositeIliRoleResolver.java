package com.interlis.transform.typesystem;

import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.RoleDef;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Viewable;
import com.interlis.transform.RoleResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CompositeIliRoleResolver implements RoleResolver {
    private final List<TransferDescription> transferDescriptions;

    public CompositeIliRoleResolver(List<TransferDescription> transferDescriptions) {
        this.transferDescriptions = List.copyOf(Objects.requireNonNull(transferDescriptions, "transferDescriptions"));
    }

    @Override
    public Optional<String> resolveRoleTarget(String viewableName, String roleName) {
        if (viewableName == null || roleName == null) {
            return Optional.empty();
        }
        for (TransferDescription td : transferDescriptions) {
            Element element = td.getElement(viewableName);
            if (!(element instanceof Viewable)) {
                continue;
            }
            Viewable<?> viewable = (Viewable<?>) element;
            RoleDef role = viewable.findRole(roleName);
            if (role == null) {
                continue;
            }
            AbstractClassDef<?> destination = role.getDestination();
            if (destination == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(destination.getScopedName(null));
        }
        return Optional.empty();
    }
}
