package com.interlis.transform.state;

import ch.interlis.iom.IomObject;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface StateStore {
    void putIdMapping(String sourceOid, String targetOid);

    Optional<String> getTargetOid(String sourceOid);

    void indexObject(String sourceClass, String basketId, IomObject object);

    Optional<IomObject> findObject(String sourceClass, String basketId, String oid);

    List<IomObject> listObjects(String sourceClass, String basketId);

    List<IomObject> findObjects(String sourceClass, String basketId, Predicate<IomObject> predicate);

    long nextObjectId();

    long nextBasketId();
}
