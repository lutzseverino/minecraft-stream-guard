package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamLink;
import java.util.Optional;

public interface StreamMetadataProvider {

    Optional<LiveStreamMetadata> metadata(StreamLink link);

    static StreamMetadataProvider none() {
        return link -> Optional.empty();
    }
}
