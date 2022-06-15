/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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

package io.helidon.webserver;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.MediaContext;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.common.MediaSupport.PredicateResult;
import io.helidon.webserver.utils.TestUtils;

import org.junit.jupiter.api.Test;

import static io.helidon.media.common.MediaSupport.reader;
import static io.helidon.webserver.utils.TestUtils.requestChunkAsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * The RequestContentTest.
 */
public class RequestContentTest {

    @Test
    public void directSubscriptionTest() {
        Request request = requestTestStub(Multi.just("first", "second", "third")
                                               .map(s -> DataChunk.create(s.getBytes())));
        StringBuilder sb = new StringBuilder();
        Multi.create(request.content()).subscribe(chunk -> sb.append(requestChunkAsString(chunk)).append("-"));
        assertThat(sb.toString(), is("first-second-third-"));
    }

    @Test
    public void upperCaseFilterTest() {
        Request request = requestTestStub(Multi.just("first", "second", "third")
                                               .map(s -> DataChunk.create(s.getBytes())));
        StringBuilder sb = new StringBuilder();
        request.content().registerFilter((Publisher<DataChunk> publisher) -> {
            sb.append("apply_filter-");
            return Multi.create(publisher)
                        .map(TestUtils::requestChunkAsString)
                        .map(String::toUpperCase)
                        .map(s -> DataChunk.create(s.getBytes()));
        });

        assertThat("Apply filter is expected to be called after a subscription!", sb.toString(), is(""));

        Multi.create(request.content())
             .subscribe(chunk -> sb.append(requestChunkAsString(chunk))
                                   .append("-"));
        assertThat(sb.toString(), is("apply_filter-FIRST-SECOND-THIRD-"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void multiThreadingFilterAndReaderTest() {
        CountDownLatch subscribedLatch = new CountDownLatch(1);
        SubmissionPublisher<DataChunk> publisher = new SubmissionPublisher<>(Runnable::run, 10);
        ForkJoinPool.commonPool()
                    .submit(() -> {
                        try {
                            if (!subscribedLatch.await(10, TimeUnit.SECONDS)) {
                                fail("Subscriber didn't subscribe in timely manner!");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted!", e);
                        }

                        publisher.submit(DataChunk.create("first".getBytes()));
                        publisher.submit(DataChunk.create("second".getBytes()));
                        publisher.submit(DataChunk.create("third".getBytes()));
                        publisher.close();
                    });

        Request request = requestTestStub(Multi.create(publisher));

        request.content()
               .registerFilter(p -> s -> p.subscribe(
                       new Subscriber<>() {
                           @Override
                           public void onSubscribe(Subscription subscription) {
                               s.onSubscribe(subscription);
                               subscribedLatch.countDown();
                           }

                           @Override
                           public void onNext(DataChunk item) {
                               // mapping the on next call only
                               s.onNext(DataChunk.create(requestChunkAsString(item).toUpperCase()
                                                                                   .getBytes()));
                           }

                           @Override
                           public void onError(Throwable throwable) {
                               s.onError(throwable);
                           }

                           @Override
                           public void onComplete() {
                               s.onComplete();
                           }
                       }));

        request.content()
               .registerReader(reader(
                       PredicateResult.supports(Iterable.class), publisher1 -> {
                           fail("Iterable reader should have not been used!");
                           throw new IllegalStateException("unreachable code");
                       }));

        request.content()
               .registerReader(reader(
                       PredicateResult.supports(ArrayList.class), publisher1 -> {
                           fail("ArrayList reader should have not been used!");
                           throw new IllegalStateException("unreachable code");
                       }));

        request.content()
               .registerReader(reader(
                       PredicateResult.supports(List.class), publisher1 -> {

                           CompletableFuture<List> future = new CompletableFuture<>();
                           List<String> list = new CopyOnWriteArrayList<>();

                           publisher1.subscribe(new Subscriber<>() {
                               @Override
                               public void onSubscribe(Subscription subscription) {
                                   subscription.request(Long.MAX_VALUE);
                                   subscribedLatch.countDown();
                               }

                               @Override
                               public void onNext(DataChunk item) {
                                   list.add(TestUtils.requestChunkAsString(item));
                               }

                               @Override
                               public void onError(Throwable throwable) {
                                   fail("Received an exception: " + throwable.getMessage());
                               }

                               @Override
                               public void onComplete() {
                                   future.complete(list);
                               }
                           });
                           return Single.create(future);
                       }));

        List<String> result = (List<String>) request.content()
                                                    .as(List.class)
                                                    .await(10, TimeUnit.SECONDS);

        assertThat(result, hasItems(is("FIRST"), is("SECOND"), is("THIRD")));
    }

    @Test
    public void failingFilter() {
        Request request = requestTestStub(Single.never());

        request.content()
               .registerFilter(publisher -> {
                   throw new IllegalStateException("failed-publisher-transformation");
               });

        request.content()
               .registerReader(reader(
                       PredicateResult.supports(Duration.class), publisher -> {
                           fail("Should not be called");
                           throw new IllegalStateException("unreachable code");
                       }));

        try {
            request.content().as(Duration.class).await(10, TimeUnit.SECONDS);
            fail("Should have thrown an exception");
        } catch (CompletionException e) {
            assertThat(e.getCause(),
                    allOf(instanceOf(IllegalStateException.class),
                            hasProperty("message", containsString("Transformation failed!"))));
            assertThat(e.getCause().getCause(),
                    hasProperty("message", containsString("failed-publisher-transformation")));
        }
    }

    @Test
    public void failingReader() {
        Request request = requestTestStub(Single.never());

        request.content()
               .registerReader(reader(
                       PredicateResult.supports(Duration.class), publisher -> {
                           throw new IllegalStateException("failed-read");
                       }));

        try {
            request.content().as(Duration.class).await(10, TimeUnit.SECONDS);
            fail("Should have thrown an exception");
        } catch (CompletionException e) {
            assertThat(e.getCause(),
                    allOf(instanceOf(IllegalStateException.class),
                            hasProperty("message", containsString("Transformation failed!"))));
            assertThat(e.getCause().getCause(),
                    hasProperty("message", containsString("failed-read")));
        }
    }

    @Test
    public void missingReaderTest() {
        Request request = requestTestStub(Single.just(DataChunk.create("hello".getBytes())));

        request.content()
               .registerReader(reader(
                       PredicateResult.supports(LocalDate.class), publisher -> {
                           throw new IllegalStateException("Should not be called");
                       }));

        try {
            request.content().as(Duration.class).await(10, TimeUnit.SECONDS);
            fail("Should have thrown an exception");
        } catch (CompletionException e) {
            assertThat(e.getCause(), instanceOf(IllegalStateException.class));
        }
    }

    @Test
    public void nullFilter() {
        Request request = requestTestStub(Single.never());
        assertThrows(NullPointerException.class, () -> request.content().registerFilter(null));
    }

    @Test
    public void failingSubscribe() {
        Request request = requestTestStub(Multi.singleton(DataChunk.create("data".getBytes())));

        request.content()
               .registerFilter(p -> {
                   throw new IllegalStateException("failed-publisher-transformation");
               });

        AtomicReference<Throwable> receivedThrowable = new AtomicReference<>();

        Multi.create(request.content())
             .subscribe(byteBuffer -> fail("Should not have been called!"), receivedThrowable::set);

        Throwable throwable = receivedThrowable.get();
        assertThat(throwable, allOf(instanceOf(IllegalArgumentException.class),
                hasProperty("message", containsString("Unexpected exception occurred during publishers chaining"))));
        assertThat(throwable.getCause(), hasProperty("message", containsString("failed-publisher-transformation")));
    }

    @Test
    public void readerTest() {
        Request request = requestTestStub(Multi.singleton(DataChunk.create("2010-01-02".getBytes())));

        Single<String> complete = request.content()
                                         .registerReader(reader(
                                                 PredicateResult.supports(LocalDate.class), (publisher, ctx) ->
                                                         ContentReaders.readString(publisher, ctx.charset())
                                                                       .map(LocalDate::parse)))
                                         .as(LocalDate.class)
                                         .map(o -> o.getDayOfMonth() + "/" + o.getMonthValue() + "/" + o.getYear());

        assertThat(complete.await(10, TimeUnit.SECONDS), is("2/1/2010"));
    }

    @Test
    public void implicitByteArrayContentReader() {
        Request request = requestTestStub(Multi.singleton(DataChunk.create("test-string".getBytes())));
        Single<String> complete = request.content().as(byte[].class).map(String::new);
        assertThat(complete.await(10, TimeUnit.SECONDS), is("test-string"));
    }

    @Test
    public void implicitStringContentReader() {
        Request request = requestTestStub(Multi.singleton(DataChunk.create("test-string".getBytes())));
        Single<String> complete = request.content().as(String.class);
        assertThat(complete.await(10, TimeUnit.SECONDS), is("test-string"));
    }

    @Test
    public void overridingStringContentReader() {
        Request request = requestTestStub(Single.just(DataChunk.create("test-string".getBytes())));

        String actual = request.content()
                               .registerReader(reader(
                                       PredicateResult.supports(String.class), publisher -> {
                                           fail("Should not be called");
                                           throw new IllegalStateException("unreachable code");
                                       }))
                               .registerReader(reader(
                                       PredicateResult.supports(String.class), publisher ->
                                               Multi.create(publisher)
                                                    .map(TestUtils::requestChunkAsString)
                                                    .map(String::toUpperCase)
                                                    .collectList()
                                                    .map((strings -> strings.get(0)))))
                               .as(String.class)
                               .await(10, TimeUnit.SECONDS);

        assertThat(actual, is("TEST-STRING"));
    }

    private static Request requestTestStub(Publisher<DataChunk> flux) {
        BareRequest bareRequestMock = mock(BareRequest.class);
        doReturn(URI.create("http://0.0.0.0:1234")).when(bareRequestMock).uri();
        doReturn(flux).when(bareRequestMock).bodyPublisher();
        WebServer webServer = mock(WebServer.class);
        MediaContext mediaContext = MediaContext.create();
        doReturn(mediaContext.readerContext()).when(webServer).readerContext();
        doReturn(mediaContext.writerContext()).when(webServer).writerContext();
        return new RequestTestStub(bareRequestMock, webServer);
    }
}
