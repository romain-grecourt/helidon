package io.helidon.media.common;

import io.helidon.common.http.MessageBody.Filter;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.StreamReader;
import io.helidon.common.http.MessageBody.StreamWriter;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBodyReaderContext;
import io.helidon.common.http.MessageBodyWriterContext;

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
                    .registerReader(StringReader.create())
                    .registerReader(ByteArrayReader.create())
                    .registerReader(InputStreamReader.create());

            // default writers
            writerContext
                    .registerWriter(ByteArrayWriter.create(/* copy */ true))
                    .registerWriter(CharSequenceWriter.create())
                    .registerWriter(ByteChannelWriter.create())
                    .registerWriter(PathWriter.create())
                    .registerWriter(FileWriter.create());
            return this;
        }

        public Builder registerReader(Reader<?> reader) {
            readerContext.registerReader(reader);
            return this;
        }

        public Builder registerStreamReader(StreamReader<?> reader) {
            readerContext.registerReader(reader);
            return this;
        }

        public Builder registerInboundFilter(Filter filter) {
            writerContext.registerFilter(filter);
            return this;
        }

        public Builder registerWriter(Writer<?> writer) {
            writerContext.registerWriter(writer);
            return this;
        }

        public Builder registerStreamWriter(StreamWriter<?> writer) {
            writerContext.registerWriter(writer);
            return this;
        }

        public Builder registerOutboundFilter(Filter filter) {
            writerContext.registerFilter(filter);
            return this;
        }

        @Override
        public MediaSupport build() {
            return new MediaSupport(readerContext, writerContext);
        }
    }
}
