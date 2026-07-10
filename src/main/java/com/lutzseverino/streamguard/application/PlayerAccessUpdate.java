package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import java.util.Objects;

public record PlayerAccessUpdate(PlayerAccessRecord expected, PlayerAccessRecord updated) {

    public PlayerAccessUpdate {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(updated, "updated");
        if (!expected.playerId().equals(updated.playerId())) {
            throw new IllegalArgumentException("A conditional update cannot change the player ID");
        }
    }
}
