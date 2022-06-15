/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.media.common;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.FormParams;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.RetrySchema;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.media.common.EntitySupport.PredicateResult;
import io.helidon.media.common.EntitySupport.Reader;
import io.helidon.media.common.EntitySupport.ReaderContext;
import io.helidon.media.common.EntitySupport.StreamWriter;
import io.helidon.media.common.EntitySupport.Writer;
import io.helidon.media.common.EntitySupport.WriterContext;

import static io.helidon.common.reactive.IoMulti.DEFAULT_RETRY_SCHEMA;
import static io.helidon.media.common.EntitySupport.reader;
import static io.helidon.media.common.EntitySupport.streamWriter;
import static io.helidon.media.common.EntitySupport.writer;

/**
 * MediaSupport that provides the default readers and writers.
 */
@SuppressWarnings("unused")
public class DefaultMediaSupport implements MediaSupport {

    private static final Writer<byte[]> BYTE_ARRAY_WRITER = writer(
            PredicateResult.supports(byte[].class), DefaultMediaSupport::writeBytes);

    private static final Writer<CharSequence> CHAR_SEQUENCE_WRITER = writer(
            PredicateResult.supports(CharSequence.class), DefaultMediaSupport::writeCharSequence);

    private static final Writer<File> FILE_WRITER =  writer(
            PredicateResult.supports(File.class), DefaultMediaSupport::writeFile);

    private static final Writer<Path> PATH_WRITER = writer(
            PredicateResult.supports(Path.class), DefaultMediaSupport::writePath);

    private static final Writer<Throwable> THROWABLE_WRITER0 = writer(
            PredicateResult.supports(Throwable.class), DefaultMediaSupport::writeThrowable0);

    private static final Writer<Throwable> THROWABLE_WRITER = writer(
            PredicateResult.supports(Throwable.class), DefaultMediaSupport::writeThrowable);

    private static final Reader<byte[]> BYTE_ARRAY_READER = reader(
            PredicateResult.supports(byte[].class), ContentReaders::readBytes);

    private static final Reader<InputStream> IS_READER = reader(
            PredicateResult.supports(InputStream.class), DefaultMediaSupport::readInputStream);

    private static final Reader<String> STRING_READER = reader(
            PredicateResult.supports(String.class), DefaultMediaSupport::readString);

    private static final StreamWriter<CharSequence> CHAR_SEQUENCE_STREAM_WRITER = streamWriter(
            PredicateResult.supports(CharSequence.class), DefaultMediaSupport::writeCharSequences);

    private static final Writer<ReadableByteChannel> BYTE_CHANNEL_WRITER = byteChannelWriter(DEFAULT_RETRY_SCHEMA);

    private static final Reader<FormParams> FORM_PARAMS_READER = reader(
            DefaultMediaSupport::acceptsFormParams, DefaultMediaSupport::readFormParams);

    private static final Writer<FormParams> FORM_PARAMS_WRITER = writer(
            DefaultMediaSupport::acceptsFormParams, DefaultMediaSupport::writeFormParams);

    private final Writer<ReadableByteChannel> byteChannelWriter;
    private final Writer<Throwable> throwableWriter;

    private DefaultMediaSupport(Builder builder) {
        if (builder.schema == null) {
            byteChannelWriter = BYTE_CHANNEL_WRITER;
        } else {
            byteChannelWriter = byteChannelWriter(builder.schema);
        }
        throwableWriter = throwableWriter(builder.includeStackTraces);
    }

    /**
     * Creates new instance of {@link DefaultMediaSupport}.
     *
     * @return new service instance
     */
    public static DefaultMediaSupport create() {
        return builder().build();
    }

    /**
     * Return new {@link Builder} of the {@link DefaultMediaSupport}.
     *
     * @return default media support builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Return {@code byte[]} reader instance.
     *
     * @return {@code byte[]} reader
     */
    public static Reader<byte[]> byteArrayReader() {
        return BYTE_ARRAY_READER;
    }

    /**
     * Return {@link String} reader instance.
     *
     * @return {@link String} reader
     */
    public static Reader<String> stringReader() {
        return STRING_READER;
    }

    /**
     * Return {@link InputStream} reader instance.
     *
     * @return {@link InputStream} reader
     */
    public static Reader<InputStream> inputStreamReader() {
        return IS_READER;
    }

    /**
     * Return {@code byte[]} writer instance.
     *
     * @return {@code byte[]} writer
     */
    public static Writer<byte[]> byteArrayWriter() {
        return BYTE_ARRAY_WRITER;
    }

    /**
     * Return {@link CharSequence} writer instance.
     *
     * @return {@link CharSequence} writer
     */
    public static Writer<CharSequence> charSequenceWriter() {
        return CHAR_SEQUENCE_WRITER;
    }

    /**
     * Return {@link CharSequence} stream writer instance.
     *
     * @return {@link CharSequence} writer
     */
    public static StreamWriter<CharSequence> charSequenceStreamWriter() {
        return CHAR_SEQUENCE_STREAM_WRITER;
    }

    /**
     * Create a new instance of {@link ReadableByteChannel} writer.
     *
     * @return {@link ReadableByteChannel} writer
     */
    public static Writer<ReadableByteChannel> byteChannelWriter() {
        return BYTE_CHANNEL_WRITER;
    }

    /**
     * Return new {@link ReadableByteChannel} writer instance with specific {@link RetrySchema}.
     *
     * @param schema retry schema
     * @return {@link ReadableByteChannel} writer
     */
    public static Writer<ReadableByteChannel> byteChannelWriter(RetrySchema schema) {
        return writer(PredicateResult.supports(ReadableByteChannel.class),
                (single, ctx) -> writeByteChannel(single, ctx, schema));
    }

    /**
     * Return {@link Path} writer instance.
     *
     * @return {@link Path} writer
     */
    public static Writer<Path> pathWriter() {
        return PATH_WRITER;
    }

    /**
     * Return {@link File} writer instance.
     *
     * @return {@link File} writer
     */
    public static Writer<File> fileWriter() {
        return FILE_WRITER;
    }

    /**
     * Return {@link FormParams} writer instance.
     *
     * @return {@link FormParams} writer
     */
    public static Writer<FormParams> formParamWriter() {
        return FORM_PARAMS_WRITER;
    }

    /**
     * Return {@link FormParams} reader instance.
     *
     * @return {@link FormParams} reader
     */
    public static Reader<FormParams> formParamReader() {
        return FORM_PARAMS_READER;
    }

    /**
     * Return {@link Throwable} writer instance.
     *
     * @param includeStackTraces whether stack traces are to be written
     * @return {@link Throwable} writer
     */
    public static Writer<Throwable> throwableWriter(boolean includeStackTraces) {
        return !includeStackTraces ? THROWABLE_WRITER0 : THROWABLE_WRITER;
    }

    @Override
    public Collection<Reader<?>> readers() {
        return List.of(
                BYTE_ARRAY_READER,
                STRING_READER,
                IS_READER,
                FORM_PARAMS_READER);
    }

    @Override
    public Collection<Writer<?>> writers() {
        return List.of(
                BYTE_ARRAY_WRITER,
                CHAR_SEQUENCE_WRITER,
                byteChannelWriter,
                PATH_WRITER,
                FILE_WRITER,
                throwableWriter,
                FORM_PARAMS_WRITER);
    }

    @Override
    public Collection<StreamWriter<?>> streamWriters() {
        return List.of(
                CHAR_SEQUENCE_STREAM_WRITER);
    }

    /**
     * Default media support builder.
     */
    public static class Builder implements io.helidon.common.Builder<Builder, DefaultMediaSupport> {

        private boolean includeStackTraces = false;
        private RetrySchema schema;

        private Builder() {
        }

        @Override
        public DefaultMediaSupport build() {
            return new DefaultMediaSupport(this);
        }

        /**
         * Whether stack traces should be included in response.
         *
         * @param includeStackTraces include stack trace
         * @return updated builder instance
         */
        public Builder includeStackTraces(boolean includeStackTraces) {
            this.includeStackTraces = includeStackTraces;
            return this;
        }

        /**
         * Set specific {@link RetrySchema} to the byte channel.
         *
         * @param schema retry schema
         * @return updated builder instance
         */
        public Builder byteChannelRetrySchema(RetrySchema schema) {
            this.schema = Objects.requireNonNull(schema);
            return this;
        }

        /**
         * Configures this {@link DefaultMediaSupport.Builder} from the supplied {@link Config}.
         * <table class="config">
         * <caption>Optional configuration parameters</caption>
         * <tr>
         *     <th>key</th>
         *     <th>description</th>
         * </tr>
         * <tr>
         *     <td>include-stack-traces</td>
         *     <td>Whether stack traces should be included in response</td>
         * </tr>
         * </table>
         *
         * @param config media support config
         * @return updated builder instance
         */
        public Builder config(Config config) {
            config.get("include-stack-traces").asBoolean().ifPresent(this::includeStackTraces);
            return this;
        }
    }

    private static Publisher<DataChunk> writeBytes(Single<byte[]> single) {
        return Single.create(single).flatMap(ContentWriters::writeBytes);
    }

    private static Publisher<DataChunk> writeCharSequences(Publisher<CharSequence> publisher, WriterContext context) {
        context.contentType(MediaType.TEXT_PLAIN);
        return Multi.create(publisher).map(s -> DataChunk.create(true, context.charset().encode(s.toString())));
    }

    private static Publisher<DataChunk> writeCharSequence(Single<CharSequence> single, WriterContext context) {
        context.contentType(MediaType.TEXT_PLAIN);
        return single.flatMap(cs -> ContentWriters.writeCharSequence(cs, context.charset()));
    }

    private static Publisher<DataChunk> writeFile(Single<File> single) {
        return single.flatMap(file -> ContentWriters.writeFile(file.toPath()));
    }

    private static Publisher<DataChunk> writePath(Single<Path> single) {
        return single.flatMap(ContentWriters::writeFile);
    }

    private static Publisher<DataChunk> writeThrowable0(Single<Throwable> single, WriterContext context) {
        context.contentType(MediaType.TEXT_PLAIN);
        return ContentWriters.writeCharSequence("Unexpected exception occurred.", context.charset());
    }

    private static Publisher<DataChunk> writeThrowable(Single<Throwable> single, WriterContext context) {
        context.contentType(MediaType.TEXT_PLAIN);
        return single.flatMap(throwable -> ContentWriters.writeStackTrace(throwable, context.charset()));
    }

    private static Publisher<DataChunk> writeByteChannel(Single<ReadableByteChannel> single,
                                                         WriterContext context,
                                                         RetrySchema schema) {
        context.contentType(MediaType.APPLICATION_OCTET_STREAM);
        return single.flatMap(channel -> ContentWriters.writeByteChannel(channel, schema));
    }

    private static Single<InputStream> readInputStream(Publisher<DataChunk> publisher) {
        return Single.just(ContentReaders.readInputStream(publisher));
    }

    private static Single<String> readString(Publisher<DataChunk> publisher, ReaderContext context) {
        return ContentReaders.readString(publisher, context.charset());
    }

    private static PredicateResult acceptsFormParams(GenericType<?> type, ReaderContext context) {
        return context.contentType()
                      .filter(mediaType -> mediaType == MediaType.APPLICATION_FORM_URLENCODED
                              || mediaType == MediaType.TEXT_PLAIN)
                      .map(it -> PredicateResult.supports(FormParams.class, type))
                      .orElse(PredicateResult.NOT_SUPPORTED);
    }

    private static PredicateResult acceptsFormParams(GenericType<?> type, WriterContext context) {
        return context.contentType()
                      .or(() -> Optional.of(MediaType.APPLICATION_FORM_URLENCODED))
                      .filter(mediaType -> mediaType == MediaType.APPLICATION_FORM_URLENCODED
                              || mediaType == MediaType.TEXT_PLAIN)
                      .map(it -> PredicateResult.supports(FormParams.class, type))
                      .orElse(PredicateResult.NOT_SUPPORTED);
    }

    private static Single<FormParams> readFormParams(Publisher<DataChunk> publisher, ReaderContext context) {
        MediaType mediaType = context.contentType().orElseThrow();
        if (mediaType.equals(MediaType.APPLICATION_FORM_URLENCODED)) {
            return ContentReaders.readURLEncodedFormParams(publisher, context.charset());
        }
        return ContentReaders.readTextPlainFormParams(publisher, context.charset());
    }

    private static Publisher<DataChunk> writeFormParams(Single<FormParams> single, WriterContext context) {
        MediaType mediaType = context.contentType().orElseGet(() -> {
            context.contentType(MediaType.APPLICATION_FORM_URLENCODED);
            return MediaType.APPLICATION_FORM_URLENCODED;
        });
        if (mediaType.equals(MediaType.APPLICATION_FORM_URLENCODED)) {
            return single.flatMap(formParams ->
                    ContentWriters.writeURLEncodedFormParams(formParams, context.charset()));
        }
        return single.flatMap(formParams ->
                ContentWriters.writePlainTextFormParams(formParams, context.charset()));
    }
}
