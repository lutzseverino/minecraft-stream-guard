package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamProviderId;

public interface StreamLinkNormalizer {

    String normalize(StreamProviderId providerId, String linkReference);
}
