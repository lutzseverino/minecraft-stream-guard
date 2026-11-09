package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamLink;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public interface StreamVerificationProvider {

  VerificationResult verify(StreamLink link);

  default Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
    Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
    for (StreamLink link : links) {
      results.computeIfAbsent(link, this::verify);
    }
    return Map.copyOf(results);
  }
}
