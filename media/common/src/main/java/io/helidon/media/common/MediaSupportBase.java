package io.helidon.media.common;

import io.helidon.common.context.Context;
import io.helidon.common.context.ContextProcessor;
import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.EntityWriters;
import java.util.Optional;

/**
 * Media support base class.
 */
public abstract class MediaSupportBase implements ContextProcessor {

    @Override
    public void processContext(Context context) {
        Optional<MediaSupport> optional = context.get(MediaSupport.class);
        if (optional.isPresent()) {
            MediaSupport mediaSupport = optional.get();
            registerReaders(mediaSupport.readers);
            registerWriters(mediaSupport.writers);
        }
    }

    protected abstract void registerWriters(EntityWriters writers);
    protected abstract void registerReaders(EntityReaders readers);
}
