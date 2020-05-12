/*
 * Copyright (c)  2020 Oracle and/or its affiliates.
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
 *
 */

package io.helidon.microprofile.messaging.inner.subscriber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;

import io.helidon.microprofile.messaging.AssertableTestBean;
import io.helidon.microprofile.messaging.AsyncTestBean;

import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

/**
 * This test is modified version of official tck test in version 1.0
 * https://github.com/eclipse/microprofile-reactive-messaging
 */
@ApplicationScoped
public class SubscriberPublMsgToMsgRetComplBean implements AssertableTestBean, AsyncTestBean {

    CopyOnWriteArraySet<String> resultData = new CopyOnWriteArraySet<>();

    private final ExecutorService executor = createExecutor();

    @Outgoing("cs-void-message")
    public Publisher<Message<String>> sourceForCsVoidMessage() {
        return ReactiveStreams.fromIterable(TEST_DATA)
                .map(Message::of)
                .buildRs();
    }

    @Incoming("cs-void-message")
    public CompletionStage<Void> consumeMessageAndReturnCompletionStageOfVoid(Message<String> message) {
        return CompletableFuture.runAsync(() -> resultData.add(message.getPayload()), executor);
    }

    @Override
    public void assertValid() {
        assertWithOrigin("Result doesn't match", resultData, is(TEST_DATA));
    }

    @Override
    public void tearDown() {
        awaitShutdown(executor);
    }
}
