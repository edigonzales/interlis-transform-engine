package com.interlis.transform.state;

import java.util.Optional;

public interface StateStore {
    void putIdMapping(String sourceOid, String targetOid);

    Optional<String> getTargetOid(String sourceOid);

    long nextObjectId();

    long nextBasketId();
}
