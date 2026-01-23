package com.interlis.transform.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryStateStore implements StateStore {
    private final Map<String, String> idMapping = new HashMap<>();
    private final AtomicLong objectIdSequence = new AtomicLong();
    private final AtomicLong basketIdSequence = new AtomicLong();

    @Override
    public void putIdMapping(String sourceOid, String targetOid) {
        idMapping.put(sourceOid, targetOid);
    }

    @Override
    public Optional<String> getTargetOid(String sourceOid) {
        return Optional.ofNullable(idMapping.get(sourceOid));
    }

    @Override
    public long nextObjectId() {
        return objectIdSequence.incrementAndGet();
    }

    @Override
    public long nextBasketId() {
        return basketIdSequence.incrementAndGet();
    }
}
