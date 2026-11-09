package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.AccessDecision;
import com.lutzseverino.streamguard.domain.GateState;
import com.lutzseverino.streamguard.domain.GuardedAction;
import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamGuardPolicy;
import java.time.Clock;
import java.util.UUID;

public final class AccessService {

  private final PlayerAccessRepository repository;
  private final SessionRegistry sessionRegistry;
  private final StreamGuardPolicy policy;
  private final Clock clock;

  public AccessService(
      PlayerAccessRepository repository,
      SessionRegistry sessionRegistry,
      StreamGuardPolicy policy,
      Clock clock) {
    this.repository = repository;
    this.sessionRegistry = sessionRegistry;
    this.policy = policy;
    this.clock = clock;
  }

  public AccessDecision decide(
      UUID playerId, String playerName, GuardedAction action, boolean permissionBypass) {
    PlayerAccessRecord accessRecord = repository.getOrCreate(playerId, playerName);
    return policy.decide(
        action,
        accessRecord,
        sessionRegistry.sessionFor(playerId),
        permissionBypass,
        clock.instant());
  }

  public GateState gateState(UUID playerId, String playerName, boolean permissionBypass) {
    PlayerAccessRecord accessRecord = repository.getOrCreate(playerId, playerName);
    return policy.gateState(accessRecord, permissionBypass, clock.instant());
  }
}
