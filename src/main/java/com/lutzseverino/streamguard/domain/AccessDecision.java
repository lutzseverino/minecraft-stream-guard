package com.lutzseverino.streamguard.domain;

public record AccessDecision(boolean allowed, Reason reason) {

  public enum Reason {
    ALLOWED,
    ACTION_NOT_GUARDED,
    VERIFIED,
    BYPASS,
    GRACE_PERIOD,
    DENIED
  }

  public static AccessDecision allow(Reason reason) {
    return new AccessDecision(true, reason);
  }

  public static AccessDecision deny() {
    return new AccessDecision(false, Reason.DENIED);
  }
}
