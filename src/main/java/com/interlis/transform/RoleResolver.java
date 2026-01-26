package com.interlis.transform;

import java.util.Optional;

@FunctionalInterface
public interface RoleResolver {
    RoleResolver NONE = (viewableName, roleName) -> Optional.empty();

    Optional<String> resolveRoleTarget(String viewableName, String roleName);
}
