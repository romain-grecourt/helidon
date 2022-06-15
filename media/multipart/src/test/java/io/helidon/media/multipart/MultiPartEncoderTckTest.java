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

package io.helidon.media.multipart;

import java.util.concurrent.Flow;
import java.util.stream.LongStream;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Multi;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import static io.helidon.media.multipart.BodyPartTest.MEDIA_CONTEXT;

public class MultiPartEncoderTckTest extends FlowPublisherVerification<DataChunk> {

    public MultiPartEncoderTckTest() {
        super(new TestEnvironment(200));
    }

    @Override
    public Flow.Publisher<DataChunk> createFlowPublisher(final long l) {
        MultiPartEncoder encoder = MultiPartEncoder.create("boundary", MEDIA_CONTEXT.writerContext());
        Multi.create(LongStream.rangeClosed(1, l)
                               .mapToObj(i ->
                                       BodyPart.builder()
                                               .entity("part" + i)
                                               .build()
                               )).subscribe(encoder);
        return encoder;
    }

    @Override
    public Flow.Publisher<DataChunk> createFailedFlowPublisher() {
        return null;
    }

    @Override
    public void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
        super.required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber();
    }

    @Test(enabled = false)
    @Override
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() {
        // disabled because the encoder always emits one more item for the closing boundary
    }

    @Test(enabled = false)
    @Override
    public void required_createPublisher1MustProduceAStreamOfExactly1Element() {
        // disabled because the encoder always emits one more item for the closing boundary
    }

    @Test(enabled = false)
    @Override
    public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() {
        // disabled because the encoder always emits one more item for the closing boundary
    }

    @Test(enabled = false)
    @Override
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() {
        // disabled because the encoder always emits one more item for the closing boundary
    }

    @Test(enabled = false)
    @Override
    public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() {
        // disabled because the encoder always emits one more item for the closing boundary
    }

    @Test(enabled = false)
    @Override
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() {
        // disabled because the encoder always emits one more item for the closing boundary
    }

    @Test(enabled = false)
    @Override
    public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() {
        // disabled because the encoder always emits one more item for the closing boundary
    }

    @Test(enabled = false)
    @Override
    public void stochastic_spec103_mustSignalOnMethodsSequentially() {
        // disabled because the encoder always emits one more item for the closing boundary
    }
}
