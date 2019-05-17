package io.helidon.media.multipart;

import java.nio.ByteBuffer;

/**
 * @author Jitendra Kotamraju
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
         * This event is issued for each header line of a part.
         * It may be generated more than once for each part.
         */
        HEADER,
        /**
         * This event is issued when the header blank line is detected. The
         * next event is {@link #CONTENT}.
         * It is generated once for each part.
         */
        END_HEADERS,
        /**
         * This event is issued for each part chunk parsed.
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

    static final StartMessage START_MESSAGE = new StartMessage();
    static final StartPart START_PART = new StartPart();
    static final EndHeaders END_HEADERS = new EndHeaders();
    static final EndPart END_PART = new EndPart();
    static final EndMessage END_MESSAGE = new EndMessage();
    static final DataRequired DATA_REQUIRED = new DataRequired();

    static final class StartMessage extends MIMEEvent {

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.START_MESSAGE;
        }
    }

    static final class StartPart extends MIMEEvent {

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.START_PART;
        }
    }

    static final class EndPart extends MIMEEvent {

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.END_PART;
        }
    }

    static final class Header extends MIMEEvent {

        private final String name;
        private final String value;

        Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.HEADER;
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }
    }

    static final class EndHeaders extends MIMEEvent {
        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.END_HEADERS;
        }
    }

    static final class Content extends MIMEEvent {

        private final ByteBuffer data;

        Content(ByteBuffer data) {
            this.data = data;
        }

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.CONTENT;
        }

        ByteBuffer getData() {
            return data;
        }
    }

    static final class EndMessage extends MIMEEvent {

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.END_MESSAGE;
        }
    }

    static final class DataRequired extends MIMEEvent {

        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.DATA_REQUIRED;
        }
    }
}
