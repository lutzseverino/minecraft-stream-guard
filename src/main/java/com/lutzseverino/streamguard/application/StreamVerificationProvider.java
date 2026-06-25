package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamLink;

public interface StreamVerificationProvider {

    VerificationResult verify(StreamLink link);
}
