package com.lutzseverino.streamguard.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

public final class StreamGuardPolicy {

    private final EnumSet<GuardedAction> guardedActions;
    private final Duration gracePeriod;

    public StreamGuardPolicy(EnumSet<GuardedAction> guardedActions, Duration gracePeriod) {
        this.guardedActions = guardedActions.isEmpty()
                ? EnumSet.noneOf(GuardedAction.class)
                : EnumSet.copyOf(guardedActions);
        this.gracePeriod = gracePeriod.isNegative() ? Duration.ZERO : gracePeriod;
    }

    public AccessDecision decide(
            GuardedAction action,
            PlayerAccessRecord record,
            SessionState session,
            boolean permissionBypass,
            Instant now
    ) {
        if (!guardedActions.contains(action)) {
            return AccessDecision.allow(AccessDecision.Reason.ACTION_NOT_GUARDED);
        }
        if (permissionBypass) {
            return AccessDecision.allow(AccessDecision.Reason.BYPASS);
        }
        if (record.verificationStatusOptional().filter(VerificationStatus::live).isPresent()) {
            return AccessDecision.allow(AccessDecision.Reason.VERIFIED);
        }
        if (record.bypassGrantOptional().filter(grant -> grant.activeAt(now)).isPresent()) {
            return AccessDecision.allow(AccessDecision.Reason.BYPASS);
        }
        if (!gracePeriod.isZero() && session != null && !session.joinedAt().plus(gracePeriod).isBefore(now)) {
            return AccessDecision.allow(AccessDecision.Reason.GRACE_PERIOD);
        }
        return AccessDecision.deny();
    }

    public GateState gateState(PlayerAccessRecord record, boolean permissionBypass, Instant now) {
        if (permissionBypass || record.bypassGrantOptional().filter(grant -> grant.activeAt(now)).isPresent()) {
            return GateState.BYPASSED;
        }
        if (record.verificationStatusOptional().filter(VerificationStatus::live).isPresent()) {
            return GateState.VERIFIED;
        }
        if (record.streamLinkOptional().isEmpty()) {
            return GateState.UNLINKED;
        }
        return GateState.NOT_LIVE;
    }
}
