package com.interlis.transform;

import java.util.Optional;

public interface StateStore {
    void putIdMapping(String sourceOid, String targetOid);

    Optional<String> getTargetOid(String sourceOid);
}
