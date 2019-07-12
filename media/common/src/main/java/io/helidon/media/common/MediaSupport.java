package io.helidon.media.common;

/**
 * Media support.
 */
public final class MediaSupport {

    private final MessageBodyReaderContext readerContext;
    private final MessageBodyWriterContext writerContext;

    private MediaSupport(MessageBodyReaderContext readerContext,
            MessageBodyWriterContext writerContext) {

        this.readerContext = readerContext;
        this.writerContext = writerContext;
    }

    public MessageBodyReaderContext readerContext() {
        return readerContext;
    }

    public MessageBodyWriterContext writerContext() {
        return writerContext;
    }

    public static MediaSupport create() {
        return builder().build();
    }

    public static MediaSupport createWithDefaults() {
        return builder().registerDefaults().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder
            implements io.helidon.common.Builder<MediaSupport> {

        private final MessageBodyReaderContext readerContext;
        private final MessageBodyWriterContext writerContext;

        Builder() {
            readerContext = MessageBodyReaderContext.create();
            writerContext = MessageBodyWriterContext.create();
        }

        public Builder registerDefaults() {
            // default readers
            readerContext
                    .registerReader(StringBodyReader.create())
                    .registerReader(ByteArrayBodyReader.create())
                    .registerReader(InputStreamBodyReader.create());

            // default writers
            writerContext
                    .registerWriter(ByteArrayBodyWriter.create(/* copy */ true))
                    .registerWriter(CharSequenceBodyWriter.create())
                    .registerWriter(ByteChannelWriter.create())
                    .registerWriter(PathBodyWriter.create())
                    .registerWriter(FileBodyWriter.create());
            return this;
        }

        public Builder registerReader(MessageBodyReader<?> reader) {
            readerContext.registerReader(reader);
            return this;
        }

        public Builder registerStreamReader(MessageBodyStreamReader<?> reader) {
            readerContext.registerReader(reader);
            return this;
        }

        public Builder registerInboundFilter(MessageBodyFilter filter) {
            writerContext.registerFilter(filter);
            return this;
        }

        public Builder registerWriter(MessageBodyWriter<?> writer) {
            writerContext.registerWriter(writer);
            return this;
        }

        public Builder registerStreamWriter(MessageBodyStreamWriter<?> writer) {
            writerContext.registerWriter(writer);
            return this;
        }

        public Builder registerOutboundFilter(MessageBodyFilter filter) {
            writerContext.registerFilter(filter);
            return this;
        }

        @Override
        public MediaSupport build() {
            return new MediaSupport(readerContext, writerContext);
        }
    }
}
