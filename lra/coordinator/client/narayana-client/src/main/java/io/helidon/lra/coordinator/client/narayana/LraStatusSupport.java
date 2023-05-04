/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.lra.coordinator.client.narayana;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.helidon.common.GenericType;
import io.helidon.common.http.Headers;
import io.helidon.common.http.HttpMediaType;
import io.helidon.common.http.WritableHeaders;
import io.helidon.nima.http.media.EntityReader;
import io.helidon.nima.http.media.MediaSupport;

import org.eclipse.microprofile.lra.annotation.LRAStatus;

class LraStatusSupport implements MediaSupport {

    private final LraStatusReader reader = new LraStatusReader();

    @SuppressWarnings("unchecked")
    <T> EntityReader<T> reader() {
        return (EntityReader<T>) reader;
    }

    boolean isSupportedType(GenericType<?> type) {
        return LRAStatus.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <T> ReaderResponse<T> reader(GenericType<T> type, Headers requestHeaders) {
        if (isSupportedType(type)) {
            return new ReaderResponse<>(SupportLevel.SUPPORTED, this::reader);
        }
        return ReaderResponse.unsupported();
    }

    @Override
    public <T> WriterResponse<T> writer(GenericType<T> type,
                                        Headers requestHeaders,
                                        WritableHeaders<?> responseHeaders) {

        return WriterResponse.unsupported();
    }

    private static final class LraStatusReader implements EntityReader<LRAStatus> {

        @Override
        public LRAStatus read(GenericType<LRAStatus> type, InputStream stream, Headers headers) {
            return read(stream, contentTypeCharset(headers));
        }

        @Override
        public LRAStatus read(GenericType<LRAStatus> type, InputStream stream, Headers requestHeaders, Headers responseHeaders) {
            return read(stream, contentTypeCharset(requestHeaders));
        }

        private LRAStatus read(InputStream in, Charset charset) {
            try {
                return LRAStatus.valueOf(new String(in.readAllBytes(), charset));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private Charset contentTypeCharset(Headers headers) {
            return headers.contentType()
                          .flatMap(HttpMediaType::charset)
                          .map(Charset::forName)
                          .orElse(StandardCharsets.UTF_8);
        }
    }
}
