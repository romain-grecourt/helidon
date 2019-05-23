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

import java.nio.ByteBuffer;

/**
 * An event represents a parser state with parsed data.
 */
abstract class MIMEEvent {

    /**
     * Represents the parser state.
     */
    enum EVENT_TYPE {
        /**
         * This event is the first event issued by the parser.
         * It is generated only once.
         */
        START_MESSAGE,
        /**
         * This event is issued when a new part is detected.
         * It is generated for each part.
         */
        START_PART,
        /**
         * This event is issued for each header line of a part. It may be
         * generated more than once for each part.
         */
        HEADER,
        /**
         * This event is issued when the header blank line is detected. The
         * next event is {@link #CONTENT}.
         * It is generated once for each part.
         */
        END_HEADERS,
        /**
         * This event is issued for each part chunk parsed. The event
         * It may be generated more than once for each part.
         */
        CONTENT,
        /**
         * This event is issued when the content for a part is complete.
         * It is generated only once for each part.
         */
        END_PART,
        /**
         * This event is issued when all parts are complete.
         * It is generated only once.
         */
        END_MESSAGE,
        /**
         * This event is issued when there is not enough data in the buffer
         * to continue parsing.
         * If issued after:
         * <ul>
         * <li>{@link #START_MESSAGE} - the parser did not detect the end of the
         * preamble</li>
         * <li>{@link #HEADER} - the parser did not detect the blank line that
         * separates the part headers and the part body</li>
         * <li>{@link #CONTENT} - the parser did not detect the next starting
         * boundary or closing boundary</li>
         * </ul>
         */
        DATA_REQUIRED
    }

    /**
     * Returns a event for the parser current cursor location.
     *
     * @see EVENT_TYPE
     * @return event type
     */
    abstract EVENT_TYPE getEventType();

    /**
     * Constant for the event of type {@link EVENT_TYPE#START_MESSAGE}.
     */
    static final StartMessage START_MESSAGE = new StartMessage();

    /**
     * Constant for the event of type {@link EVENT_TYPE#START_PART}.
     */
    static final StartPart START_PART = new StartPart();

    /**
     * Constant for the event of type {@link EVENT_TYPE#END_HEADERS}.
     */
    static final EndHeaders END_HEADERS = new EndHeaders();

    /**
     * Constant for the event of type {@link EVENT_TYPE#END_PART}.
     */
    static final EndPart END_PART = new EndPart();

    /**
     * Constant for the event of type {@link EVENT_TYPE#END_MESSAGE}.
     */
    static final EndMessage END_MESSAGE = new EndMessage();

    /**
     * Constant for the event of type {@link EVENT_TYPE#DATA_REQUIRED}.
     */
    static final DataRequired DATA_REQUIRED = new DataRequired();

    /**
     * The event class for {@link EVENT_TYPE#START_MESSAGE}.
     * Use {@link #START_MESSAGE} to access the singleton instance.
     */
    static final class StartMessage extends MIMEEvent {

        /**
         * Cannot be instanciated.
         */
        private StartMessage() {
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.START_MESSAGE;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#START_PART}.
     * Use {@link #START_PART} to access the singleton instance.
     */
    static final class StartPart extends MIMEEvent {

        /**
         * Cannot be instanciated.
         */
        private StartPart() {
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.START_PART;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#END_PART}.
     * Use {@link #END_PART} to access the singleton instance.
     */
    static final class EndPart extends MIMEEvent {

        /**
         * Cannot be instanciated.
         */
        private EndPart() {
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.END_PART;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#HEADER}. This event contains parsed
     * header name and value, see {@link #getName()} and {@link #getValue()}
     */
    static final class Header extends MIMEEvent {

        private final String name;
        private final String value;

        /**
         * Create a new instance.
         * @param name header name
         * @param value header value
         */
        Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.HEADER;
        }

        /**
         * Get the parsed header name.
         * @return header name
         */
        String getName() {
            return name;
        }

        /**
         * Get the parsed header value.
         * @return header value
         */
        String getValue() {
            return value;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#END_HEADERS}.
     * Use {@link #END_HEADERS} to access the singleton instance.
     */
    static final class EndHeaders extends MIMEEvent {

        /**
         * Cannot be instanciated.
         */
        private EndHeaders() {
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.END_HEADERS;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#CONTENT}.
     * This event contains parsed data of a part body, see {@link #getData()}.
     */
    static final class Content extends MIMEEvent {

        private final ByteBuffer data;

        /**
         * Create a new instance.
         * @param data parsed data
         */
        Content(ByteBuffer data) {
            this.data = data;
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.CONTENT;
        }

        /**
         * Get the parsed part body data.
         * @return ByteBuffer
         */
        ByteBuffer getData() {
            return data;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#END_MESSAGE}.
     * Use {@link #END_MESSAGE} to access the singleton instance.
     */
    static final class EndMessage extends MIMEEvent {

        /**
         * Cannot be instanciated.
         */
        private EndMessage() {
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.END_MESSAGE;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#DATA_REQUIRED}.
     * Use {@link #DATA_REQUIRED} to access the singleton instance.
     */
    static final class DataRequired extends MIMEEvent {

        /**
         * Cannot be instanciated.
         */
        private DataRequired() {
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.DATA_REQUIRED;
        }
    }
}
