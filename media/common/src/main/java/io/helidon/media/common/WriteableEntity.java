/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import io.helidon.media.common.EntitySupport.ReaderContext;
import io.helidon.media.common.EntitySupport.StreamWriter;
import io.helidon.media.common.EntitySupport.Writer;
import io.helidon.media.common.EntitySupport.WriterContext;
import io.helidon.media.common.EntitySupport.Writers;

/**
 * {@link Payload} that can be generated from objects.
 */
public interface WriteableEntity extends Payload, Writers {

    /**
     * Get the writer context.
     *
     * @return this instance
     */
    WriterContext writerContext();

    /**
     * Set the writer context to use for marshalling.
     *
     * @param context writer context
     * @return this instance
     */
    WriteableEntity writerContext(WriterContext context);

    /**
     * Set the reader context to use for unmarshalling.
     *
     * @param context reader context
     * @return this instance
     */
    WriteableEntity readerContext(ReaderContext context);

    @Override
    default WriteableEntity registerWriter(Writer<?> writer) {
        writerContext().registerWriter(writer);
        return this;
    }

    @Override
    default WriteableEntity registerWriter(StreamWriter<?> writer) {
        writerContext().registerWriter(writer);
        return this;
    }
}
