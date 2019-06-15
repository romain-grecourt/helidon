package io.helidon.media.common;

import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.EntityWriters;

/**
 * Media support.
 */
public final class MediaSupport {

    final EntityReaders readersSupport;
    final EntityWriters writersSupport;

    public MediaSupport() {
        readersSupport = new EntityReaders();
        writersSupport = new EntityWriters();
    }

    public MediaSupport(MediaSupport delegate) {
        readersSupport = new EntityReaders(delegate.readersSupport);
        writersSupport = new EntityWriters(delegate.writersSupport);
    }

    public void registerDefaults() {
        // default readers
        readersSupport.registerReader(new StringEntityReader());
        readersSupport.registerReader(new ByteArrayEntityReader());
        readersSupport.registerReader(new InputStreamEntityReader());

        // default writers
        writersSupport.registerWriter(new ByteArrayCopyEntityWriter());
        writersSupport.registerWriter(new CharSequenceEntityWriter());
        writersSupport.registerWriter(new ByteChannelEntityWriter());
        writersSupport.registerWriter(new PathEntityWriter());
        writersSupport.registerWriter(new FileEntityWriter());
    }
}
