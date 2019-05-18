/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.media.multipart;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.media.multipart.MultiPartSupport.BodyPartSubscriber;
import io.helidon.webserver.HashRequestHeaders;
import io.helidon.webserver.RequestHeaders;
import io.helidon.webserver.Request;
import io.helidon.webserver.Response;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

import static io.helidon.common.CollectionsHelper.listOf;
import static io.helidon.common.CollectionsHelper.mapOf;
import static io.helidon.media.multipart.BodyPartTest.READERS;
import static io.helidon.media.multipart.BodyPartTest.WRITERS;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;
import org.mockito.Mockito;

/**
 * Tests {@link BodyPartStreamReader}.
 */
public class BodyPartStreamReaderTest {

    @Test
    public void testOnePartInOneChunk() {
        String boundary = "boundary";
        final byte[] chunk1 = ("--" + boundary + "\n"
                + "Content-Id: part1\n"
                + "\n"
                + "body 1\n"
                + "--" + boundary + "--").getBytes();

        try {
            Collection<BodyPart> parts = readParts(boundary, chunk1);
            assertThat(parts.size(), is(equalTo(1)));
            BodyPart part1 = parts.iterator().next();
            assertThat(part1.headers().values("Content-Id"), hasItems("part1"));
            String part1Body = part1.content().as(String.class)
                    .toCompletableFuture()
                    .get();
            assertThat(part1Body, is(equalTo("body 1")));
        } catch (InterruptedException ex) {
            fail(ex);
        } catch (ExecutionException ex) {
            fail(ex.getCause());
        }
    }

    // TODO multiple parts in one chunk
    // TODO part across chunks
    // TODO part with multiple chunks before content
    // TODO part with content across multiple chunks 
    // TODO part subscriber requesting one by one
    // TODO part content subscriber requested part chunk one by one

    /**
     * Read the specified request chunks as a {@code <Collection<BodyPart>>}
     * using {@link BodyPartStreamReader}.
     *
     * @param chunks request chunks
     * @return collection of {@link BodyPart}
     */
    private static Collection<BodyPart> readParts(
            String boundary, byte[]... chunks)
            throws InterruptedException, ExecutionException {

        BodyPartSubscriber partsSubscriber = new BodyPartSubscriber();
        RequestHeaders headers = new HashRequestHeaders(mapOf("Content-Type",
                listOf("multipart/form-data ; boundary=" + boundary)));
        new BodyPartStreamReader(mockRequest(headers), mockResponse())
                .apply(new DataChunkPublisher(chunks))
                .subscribe(partsSubscriber);
        return partsSubscriber.getFuture().get();
    }

    /**
     * A publisher that publishes data chunks from a predefined set of byte
     * arrays.
     */
    static final class DataChunkPublisher implements Publisher<DataChunk> {

        private final Queue<DataChunk> queue = new LinkedList<>();
        private long requested;
        private boolean canceled;
        private boolean complete;

        public DataChunkPublisher(byte[]... chunksData) {
            canceled = false;
            requested = 0;
            for (byte[] chunkData : chunksData) {
                queue.add(DataChunk.create(chunkData));
            }
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0 || canceled || complete) {
                        return;
                    }
                    requested += n;
                    while (!complete && requested > 0){
                        DataChunk chunk = queue.poll();
                        if (chunk != null) {
                            requested--;
                            if (queue.isEmpty()){
                                complete = true;
                            }
                            subscriber.onNext(chunk);
                        }
                    }
                    if (complete) {
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    canceled = true;
                }
            });
        }
    }

    static Response mockResponse() {
        Response responseMock = Mockito.mock(Response.class);
        Mockito.doReturn(WRITERS).when(responseMock).getWriters();
        return responseMock;
    }

    static Request mockRequest(RequestHeaders headers) {
        Request requestMock = Mockito.mock(Request.class);
        Request.Content contentMock = Mockito.mock(Request.Content.class);
        Mockito.doReturn(READERS).when(contentMock).getReaders();
        Mockito.doReturn(contentMock).when(requestMock).content();
        Mockito.doReturn(headers).when(requestMock).headers();
        return requestMock;
    }
}
