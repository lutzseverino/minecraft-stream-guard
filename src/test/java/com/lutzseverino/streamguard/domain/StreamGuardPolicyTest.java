package com.lutzseverino.streamguard.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class StreamGuardPolicyTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-24T18:00:00Z");

    @Test
    void unlinkedPlayerCannotPerformGuardedWorldActionAfterGracePeriod() {
        StreamGuardPolicy policy = new StreamGuardPolicy(EnumSet.of(GuardedAction.BLOCK_BREAK), Duration.ofSeconds(5));
        PlayerAccessRecord record = PlayerAccessRecord.empty(PLAYER_ID, "Lutz");
        SessionState session = new SessionState(PLAYER_ID, NOW.minusSeconds(10));

        AccessDecision decision = policy.decide(GuardedAction.BLOCK_BREAK, record, session, false, NOW);

        assertFalse(decision.allowed());
        assertEquals(AccessDecision.Reason.DENIED, decision.reason());
    }

    @Test
    void gracePeriodAllowsGuardedActionsTemporarily() {
        StreamGuardPolicy policy = new StreamGuardPolicy(EnumSet.of(GuardedAction.BLOCK_PLACE), Duration.ofSeconds(30));
        PlayerAccessRecord record = PlayerAccessRecord.empty(PLAYER_ID, "Lutz");
        SessionState session = new SessionState(PLAYER_ID, NOW.minusSeconds(10));

        AccessDecision decision = policy.decide(GuardedAction.BLOCK_PLACE, record, session, false, NOW);

        assertTrue(decision.allowed());
        assertEquals(AccessDecision.Reason.GRACE_PERIOD, decision.reason());
    }

    @Test
    void liveVerificationAllowsGuardedActions() {
        StreamGuardPolicy policy = new StreamGuardPolicy(EnumSet.of(GuardedAction.ENTITY_DAMAGE), Duration.ZERO);
        PlayerAccessRecord record = PlayerAccessRecord.empty(PLAYER_ID, "Lutz")
                .withVerificationStatus(VerificationStatus.live(StreamProviderId.TWITCH, NOW, "live"));

        AccessDecision decision = policy.decide(GuardedAction.ENTITY_DAMAGE, record, null, false, NOW);

        assertTrue(decision.allowed());
        assertEquals(AccessDecision.Reason.VERIFIED, decision.reason());
    }

    @Test
    void expiredBypassDoesNotAllowGuardedActions() {
        StreamGuardPolicy policy = new StreamGuardPolicy(EnumSet.of(GuardedAction.ITEM_DROP), Duration.ZERO);
        PlayerAccessRecord record = PlayerAccessRecord.empty(PLAYER_ID, "Lutz")
                .withBypassGrant(new BypassGrant(PLAYER_ID, null, NOW.minusSeconds(60), NOW.minusSeconds(1), "test"));

        AccessDecision decision = policy.decide(GuardedAction.ITEM_DROP, record, null, false, NOW);

        assertFalse(decision.allowed());
        assertEquals(GateState.UNLINKED, policy.gateState(record, false, NOW));
    }

    @Test
    void linkedButOfflinePlayerHasNotLiveGateState() {
        StreamGuardPolicy policy = new StreamGuardPolicy(EnumSet.allOf(GuardedAction.class), Duration.ZERO);
        PlayerAccessRecord record = PlayerAccessRecord.empty(PLAYER_ID, "Lutz")
                .withStreamLink(new StreamLink(StreamProviderId.YOUTUBE, "@channel"))
                .withVerificationStatus(VerificationStatus.unverified(NOW, "offline"));

        assertEquals(GateState.NOT_LIVE, policy.gateState(record, false, NOW));
    }
}
