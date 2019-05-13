package io.helidon.media.multipart;

import java.nio.ByteBuffer;

/**
 * @author Jitendra Kotamraju
 */
abstract class MIMEEvent {

    enum EVENT_TYPE {
        START_MESSAGE,
        START_PART,
        HEADER,
        END_HEADERS,
        CONTENT,
        END_PART,
        END_MESSAGE,
        DATA_REQUIRED
    }

    /**
     * Returns a event for the parser current cursor location in the MIME
     * message.
     *
     * <p>
     * {@link EVENT_TYPE#START_MESSAGE} and {@link EVENT_TYPE#START_MESSAGE}
     * events are generated only once.
     *
     * <p>
     * {@link EVENT_TYPE#START_PART}, {@link EVENT_TYPE#END_PART} events are
     * generated only once for each attachment part.
     *
     * <p>
     * {@link EVENT_TYPE#END_HEADERS} event may be generated only once for each
     * attachment part.
     *
     * <p>
     * {@link EVENT_TYPE#CONTENT}, {@link EVENT_TYPE#HEADER} events may be
     * generated more than once for an attachment part.
     *
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
