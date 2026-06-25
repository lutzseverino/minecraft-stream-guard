package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.SessionState;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionRegistry {

    private final Clock clock;
    private final Map<UUID, SessionState> sessions = new ConcurrentHashMap<>();

    public SessionRegistry(Clock clock) {
        this.clock = clock;
    }

    public void playerJoined(UUID playerId) {
        sessions.put(playerId, new SessionState(playerId, clock.instant()));
    }

    public void playerLeft(UUID playerId) {
        sessions.remove(playerId);
    }

    public SessionState sessionFor(UUID playerId) {
        return sessions.get(playerId);
    }
}
