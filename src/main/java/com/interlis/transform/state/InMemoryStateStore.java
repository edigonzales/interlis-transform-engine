package com.interlis.transform.state;

import ch.interlis.iom.IomObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class InMemoryStateStore implements StateStore {
    private final Map<String, String> idMapping = new HashMap<>();
    private final Map<String, Map<String, Map<String, IomObject>>> objectIndex = new HashMap<>();
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
    public void indexObject(String sourceClass, String basketId, IomObject object) {
        String safeBasketId = basketId == null ? "" : basketId;
        String objectKey = object.getobjectoid();
        if (objectKey == null) {
            objectKey = Long.toString(objectIdSequence.incrementAndGet());
        }
        objectIndex
                .computeIfAbsent(sourceClass, key -> new HashMap<>())
                .computeIfAbsent(safeBasketId, key -> new HashMap<>())
                .put(objectKey, object);
    }

    @Override
    public Optional<IomObject> findObject(String sourceClass, String basketId, String oid) {
        if (oid == null) {
            return Optional.empty();
        }
        String safeBasketId = basketId == null ? "" : basketId;
        return Optional.ofNullable(objectIndex.getOrDefault(sourceClass, Map.of())
                .getOrDefault(safeBasketId, Map.of())
                .get(oid));
    }

    @Override
    public List<IomObject> listObjects(String sourceClass, String basketId) {
        String safeBasketId = basketId == null ? "" : basketId;
        Map<String, IomObject> objects = objectIndex.getOrDefault(sourceClass, Map.of())
                .getOrDefault(safeBasketId, Map.of());
        return new ArrayList<>(objects.values());
    }

    @Override
    public List<IomObject> findObjects(String sourceClass, String basketId, Predicate<IomObject> predicate) {
        return listObjects(sourceClass, basketId).stream()
                .filter(predicate)
                .collect(Collectors.toList());
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
