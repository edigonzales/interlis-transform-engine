package com.interlis.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryStateStore implements StateStore {
    private final Map<String, String> idMapping = new HashMap<>();

    @Override
    public void putIdMapping(String sourceOid, String targetOid) {
        idMapping.put(sourceOid, targetOid);
    }

    @Override
    public Optional<String> getTargetOid(String sourceOid) {
        return Optional.ofNullable(idMapping.get(sourceOid));
    }
}
