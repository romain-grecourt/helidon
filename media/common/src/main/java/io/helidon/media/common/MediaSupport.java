package io.helidon.media.common;

import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.EntityWriters;

/**
 * Media support.
 */
public final class MediaSupport {

    final EntityReaders readers;
    final EntityWriters writers;

    public MediaSupport() {
        readers = new EntityReaders();
        writers = new EntityWriters();
    }

    public MediaSupport(MediaSupport delegate) {
        readers = new EntityReaders(delegate.readers);
        writers = new EntityWriters(delegate.writers);
    }

    public MediaSupport registerDefaults() {
        // default readers
        readers.registerReader(new StringEntityReader());
        readers.registerReader(new ByteArrayEntityReader());
        readers.registerReader(new InputStreamEntityReader());

        // default writers
        writers.registerWriter(new ByteArrayCopyEntityWriter());
        writers.registerWriter(new CharSequenceEntityWriter());
        writers.registerWriter(new ByteChannelEntityWriter());
        writers.registerWriter(new PathEntityWriter());
        writers.registerWriter(new FileEntityWriter());
        return this;
    }

    public EntityReaders readers() {
        return readers;
    }

    public EntityWriters writers() {
        return writers;
    }
}
