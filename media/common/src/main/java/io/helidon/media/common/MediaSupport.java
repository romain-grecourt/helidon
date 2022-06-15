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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.MediaContext.OperatorContext;
import io.helidon.media.common.MediaContext.ReaderContext;
import io.helidon.media.common.MediaContext.Readers;
import io.helidon.media.common.MediaContext.WriterContext;
import io.helidon.media.common.MediaContext.Writers;

/**
 * Service used to register readers and writers to the respective context.
 * <p>
 * MediaSupport instances can be used with WebServer and WebClient to register readers and writer.
 * Each of these have method addMediaSupport(), which will add corresponding support.
 * </p><br>
 * WebServer example usage:
 * <pre><code>
 * WebServer.builder()
 *          .addMediaSupport(JsonbSupport.create())
 *          .build();
 * </code></pre>
 * WebClient example usage:
 * <pre><code>
 * WebClient.builder()
 *          .addMediaSupport(JacksonSupport.create())
 *          .build();
 * </code></pre>
 * If you need to register MediaSupport on the request or response, you will need to register them to
 * the corresponding context.
 * <br>
 * Example request reader registration:
 * <pre><code>
 * Routing.builder()
 *        .get("/foo", (res, req) -&gt; {
 *            ReadableEntity content = req.content();
 *            content.registerReader(JsonbSupport.create())
 *            content.as(String.class)
 *                   .thenAccept(System.out::print);
 *        })
 * </code></pre>
 * Example response writer registration:
 * <pre><code>
 * Routing.builder()
 *        .get("/foo", (res, req) -&gt; {
 *           EntitySupport.WriterContext writerContext = res.writerContext();
 *           writerContext.registerWriter(JsonbSupport.create())
 *           res.send("Example entity");
 *        })
 * </code></pre>
 */
@SuppressWarnings("unused")
public interface MediaSupport {

    /**
     * Create a new writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return writer
     */
    static <T> Writer<T> writer(OperatorPredicate<WriterContext> predicate, WriterFunction<T> function) {
        return new FunctionalWriter<>(predicate, function);
    }

    /**
     * Create a new writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return writer
     */
    static <T> Writer<T> writer(OperatorPredicate<WriterContext> predicate,
                                Function<Single<T>, Flow.Publisher<DataChunk>> function) {

        return new FunctionalWriter<>(predicate, (single, ctx) -> function.apply(single));
    }

    /**
     * Create a new stream writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return stream writer
     */
    static <T> StreamWriter<T> streamWriter(OperatorPredicate<WriterContext> predicate,
                                            StreamWriterFunction<T> function) {

        return new FunctionalStreamWriter<>(predicate, function);
    }

    /**
     * Create a new stream writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return stream writer
     */
    static <T> StreamWriter<T> streamWriter(OperatorPredicate<WriterContext> predicate,
                                            Function<Flow.Publisher<T>, Flow.Publisher<DataChunk>> function) {

        return new FunctionalStreamWriter<>(predicate, (publisher, ctx) -> function.apply(publisher));
    }

    /**
     * Create a new reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return reader
     */
    static <T> Reader<T> reader(OperatorPredicate<ReaderContext> predicate, ReaderFunction<T> function) {
        return new FunctionalReader<>(predicate, function);
    }

    /**
     * Create a new reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return reader
     */
    static <T> Reader<T> reader(OperatorPredicate<ReaderContext> predicate,
                                Function<Flow.Publisher<DataChunk>, Single<T>> function) {

        return new FunctionalReader<>(predicate, (publisher, ctx) -> function.apply(publisher));
    }

    /**
     * Create a new stream reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return stream reader
     */
    static <T> StreamReader<T> streamReader(OperatorPredicate<ReaderContext> predicate,
                                            StreamReaderFunction<T> function) {

        return new FunctionalStreamReader<>(predicate, function);
    }

    /**
     * Create a new stream reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return stream reader
     */
    static <T> StreamReader<T> streamReader(OperatorPredicate<ReaderContext> predicate,
                                            Function<Flow.Publisher<DataChunk>, Multi<T>> function) {

        return new FunctionalStreamReader<>(predicate, (publisher, ctx) -> function.apply(publisher));
    }

    /**
     * Register readers and writers.
     *
     * @param readersRegistry readers registry
     * @param writersRegistry writers registry
     */
    default void register(Readers readersRegistry, Writers writersRegistry) {
        readers().forEach(readersRegistry::registerReader);
        writers().forEach(writersRegistry::registerWriter);
        streamReaders().forEach(readersRegistry::registerReader);
        streamWriters().forEach(writersRegistry::registerWriter);
    }

    /**
     * Returns the collection of the readers which should be registered.
     *
     * @return readers
     */
    default Collection<Reader<?>> readers() {
        return Collections.emptyList();
    }

    /**
     * Returns the collection of the writers which should be registered.
     *
     * @return writers
     */
    default Collection<Writer<?>> writers() {
        return Collections.emptyList();
    }

    /**
     * Returns the collection of the stream readers which should be registered.
     *
     * @return stream readers
     */
    default Collection<StreamReader<?>> streamReaders() {
        return Collections.emptyList();
    }

    /**
     * Returns the collection of the stream writers which should be registered.
     *
     * @return stream writers
     */
    default Collection<StreamWriter<?>> streamWriters() {
        return Collections.emptyList();
    }

    /**
     * Status whether requested class type is supported by the operator.
     */
    enum PredicateResult {

        /**
         * entity type not supported.
         */
        NOT_SUPPORTED,

        /**
         * entity type is compatible with this operator, but it is not exact match.
         */
        COMPATIBLE,

        /**
         * entity type is supported by that specific operator.
         */
        SUPPORTED;

        /**
         * Whether handled class is supported.
         * Method {@link Class#isAssignableFrom(Class)} is invoked to verify if class under expected parameter is
         * supported by by the class under actual parameter.
         *
         * @param expected expected type
         * @param actual   actual type
         * @return if supported or not
         */
        public static PredicateResult supports(Class<?> expected, GenericType<?> actual) {
            return expected.isAssignableFrom(actual.rawType()) ? SUPPORTED : NOT_SUPPORTED;
        }

        /**
         * Create a predicate function that tests if the given type is supported.
         *
         * @param expected expected type
         * @return predicate function
         */
        public static <U extends OperatorContext> OperatorPredicate<U> supports(Class<?> expected) {
            return (type, ctx) -> supports(expected, type);
        }

        /**
         * Create a predicate function that tests if the combination of a given type and content-type is supported.
         *
         * @param expected    expected type
         * @param contentType expected content-type
         * @return predicate function
         */
        public static <U extends OperatorContext> OperatorPredicate<U> supports(Class<?> expected,
                                                                                MediaType contentType) {

            return (type, ctx) -> ctx.contentType()
                                     .filter(contentType::equals)
                                     .map(it -> supports(expected, type))
                                     .orElse(NOT_SUPPORTED);
        }
    }

    /**
     * Media operator that can be selected based on a entity type and a context.
     *
     * @param <T> Type supported by the operator
     */
    interface OperatorPredicate<T extends MediaContext.OperatorContext> {

        /**
         * Test if the operator can convert the given type.
         *
         * @param type    the entity type
         * @param context the operator context
         * @return {@link PredicateResult} result
         */
        PredicateResult accept(GenericType<?> type, T context);
    }

    /**
     * Function to filter or replace {@link DataChunk} publisher.
     * It can be used for various purposes, for example data coding, logging, filtering, caching, etc.
     */
    interface Filter extends Function<Flow.Publisher<DataChunk>, Flow.Publisher<DataChunk>> {
    }

    /**
     * Media operator that can convert raw data into one object.
     *
     * @param <T> type or base type supported by the operator
     */
    interface Reader<T> extends OperatorPredicate<ReaderContext> {

        /**
         * Convert raw data into a Flow.Publisher of the given type.
         *
         * @param chunks  raw data
         * @param type    entity type
         * @param context reader context
         * @param <U>     entity type
         * @return Single
         */
        <U extends T> Single<U> read(Flow.Publisher<DataChunk> chunks, GenericType<U> type, ReaderContext context);

        /**
         * Create a function that can be used to an object from raw data and a type.
         *
         * @param publisher raw data
         * @param type      entity type
         * @return Unmarshalling function
         */
        default Function<ReaderContext, Single<T>> unmarshall(Flow.Publisher<DataChunk> publisher, Class<T> type) {
            return ctx -> ctx.unmarshall(publisher, this, GenericType.create(type));
        }

        /**
         * Create a function that can be used to an object from raw data and a type.
         *
         * @param publisher raw data
         * @param type      entity type
         * @return Unmarshalling function
         */
        default Function<ReaderContext, Single<T>> unmarshall(Flow.Publisher<DataChunk> publisher,
                                                              GenericType<T> type) {

            return ctx -> ctx.unmarshall(publisher, this, type);
        }
    }

    /**
     * Media operator that can convert raw data into a stream of objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface StreamReader<T> extends OperatorPredicate<ReaderContext> {

        /**
         * Convert raw data into a Flow.Publisher of the given type.
         *
         * @param publisher raw data
         * @param type      entity type
         * @param context   reader context
         * @param <U>       entity type
         * @return Multi
         */
        <U extends T> Multi<U> read(Flow.Publisher<DataChunk> publisher, GenericType<U> type, ReaderContext context);

        /**
         * Create a function that can be used to a stream of objects from raw data and a type.
         *
         * @param publisher raw data
         * @param type      entity type
         * @return Unmarshalling function
         */
        default Function<ReaderContext, Flow.Publisher<T>> unmarshall(Flow.Publisher<DataChunk> publisher,
                                                                      Class<T> type) {

            return ctx -> ctx.unmarshallStream(publisher, this, GenericType.create(type));
        }

        /**
         * Create a function that can be used to a stream of objects from raw data and a type.
         *
         * @param publisher raw data
         * @param type      entity type
         * @return Unmarshalling function
         */
        default Function<ReaderContext, Flow.Publisher<T>> unmarshall(Flow.Publisher<DataChunk> publisher,
                                                                      GenericType<T> type) {

            return ctx -> ctx.unmarshallStream(publisher, this, type);
        }
    }

    /**
     * Media operator that generates raw data from objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface Writer<T> extends OperatorPredicate<WriterContext> {

        /**
         * Generate raw data from the specified object of the given type.
         *
         * @param single  object to be converted
         * @param type    entity type
         * @param context the writer context
         * @return {@link DataChunk} publisher
         */
        <U extends T> Flow.Publisher<DataChunk> write(Single<U> single, GenericType<U> type, WriterContext context);

        /**
         * Create a function that can be used to generate raw data from an object.
         *
         * @param value value to marshall
         * @return Marshalling function
         */
        default Function<WriterContext, Flow.Publisher<DataChunk>> marshall(T value) {
            return ctx -> ctx.marshall(Single.just(value), this, GenericType.create(value));
        }
    }

    /**
     * Media operator that generate raw data from a stream of objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface StreamWriter<T> extends OperatorPredicate<WriterContext> {

        /**
         * Generate raw data from the specified stream of objects of the given type.
         *
         * @param publisher entity stream
         * @param type      entity type
         * @param context   the writer context
         * @return {@link DataChunk} publisher
         */
        <U extends T> Flow.Publisher<DataChunk> write(Flow.Publisher<U> publisher,
                                                      GenericType<U> type,
                                                      WriterContext context);

        /**
         * Create a function that can be used to generate raw data from a stream of objects.
         *
         * @param publisher entity stream
         * @param type      entity type
         * @return Marshalling function
         */
        default Function<WriterContext, Flow.Publisher<DataChunk>> marshall(Flow.Publisher<T> publisher,
                                                                            Class<T> type) {

            return ctx -> ctx.marshallStream(publisher, this, GenericType.create(type));
        }

        /**
         * Create a function that can be used to generate raw data from a stream of objects.
         *
         * @param publisher entity stream
         * @param type      entity type
         * @return Marshalling function
         */
        default Function<WriterContext, Flow.Publisher<DataChunk>> marshall(Flow.Publisher<T> publisher,
                                                                            GenericType<T> type) {

            return ctx -> ctx.marshallStream(publisher, this, type);
        }
    }

    /**
     * Function that can convert raw data into one object.
     *
     * @param <T> entity type
     */
    interface ReaderFunction<T> extends BiFunction<Flow.Publisher<DataChunk>, ReaderContext, Single<T>> {
    }

    /**
     * Function that can convert raw data into a stream of object.
     *
     * @param <T> entity type
     */
    interface StreamReaderFunction<T> extends BiFunction<Flow.Publisher<DataChunk>, ReaderContext, Multi<T>> {
    }

    /**
     * Function that can generate raw data from an object.
     *
     * @param <T> entity type
     */
    interface WriterFunction<T> extends BiFunction<Single<T>, WriterContext, Flow.Publisher<DataChunk>> {
    }

    /**
     * Function that can generate raw data from a stream of object.
     *
     * @param <T> entity type
     */
    interface StreamWriterFunction<T> extends BiFunction<Flow.Publisher<T>, WriterContext, Flow.Publisher<DataChunk>> {
    }
}
