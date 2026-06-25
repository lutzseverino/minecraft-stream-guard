package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import java.util.Optional;
import java.util.UUID;

public interface PlayerAccessRepository {

    PlayerAccessRecord getOrCreate(UUID playerId, String playerName);

    Optional<PlayerAccessRecord> find(UUID playerId);

    void save(PlayerAccessRecord record);
}
