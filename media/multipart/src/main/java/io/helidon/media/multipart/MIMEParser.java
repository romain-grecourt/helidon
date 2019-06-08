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

import io.helidon.common.http.Utils;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Parser for multipart MIME message.
 */
final class MIMEParser {

    /**
     * The emitted parser event types.
     */
    static enum EVENT_TYPE {

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
         * This event is issued for each header line of a part. It may be
         * generated more than once for each part.
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
         * This event is issued when all parts are complete. It is generated
         * only once.
         */
        END_MESSAGE,

        /**
         * This event is issued when there is not enough data in the buffer to
         * continue parsing. If issued after:
         * <ul>
         * <li>{@link #START_MESSAGE} - the parser did not detect the end of
         * the preamble</li>
         * <li>{@link #HEADER} - the parser
         * did not detect the blank line that separates the part headers and the
         * part body</li>
         * <li>{@link #CONTENT} - the parser did not
         * detect the next starting boundary or closing boundary</li>
         * </ul>
         */
        DATA_REQUIRED
    }

    /**
     * Base class for the parser events.
     */
    static abstract class ParserEvent {

        /**
         * Get the event type.
         * @return EVENT_TYPE
         */
        abstract EVENT_TYPE type();

        /**
         * Get this event as a {@link HeaderEvent}.
         * @return HeaderEvent
         */
        HeaderEvent asHeaderEvent() {
            return (HeaderEvent) this;
        }

        /**
         * Get this event as a {@link ContentEvent}.
         *
         * @return ContentEvent
         */
        ContentEvent asContentEvent() {
            return (ContentEvent) this;
        }

        /**
         * Get this event as a {@link DataRequiredEvent}.
         *
         * @return DataRequiredEvent
         */
        DataRequiredEvent asDataRequiredEvent() {
            return (DataRequiredEvent) this;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#START_MESSAGE}.
     */
    static final class StartMessageEvent extends ParserEvent {

        private StartMessageEvent() {
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.START_MESSAGE;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#START_MESSAGE}.
     */
    static final class StartPartEvent extends ParserEvent {

        private StartPartEvent() {
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.START_PART;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#HEADER}.
     */
    static final class HeaderEvent extends ParserEvent {

        private final String name;
        private final String value;

        private HeaderEvent(String name, String value) {
            this.name = name;
            this.value = value;
        }

        String name() {
            return name;
        }

        String value() {
            return value;
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.HEADER;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#END_HEADERS}.
     */
    static final class EndHeadersEvent extends ParserEvent {

        private EndHeadersEvent() {
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.END_HEADERS;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#CONTENT}.
     */
    static final class ContentEvent extends ParserEvent {

        private final ByteBuffer data;

        ContentEvent(ByteBuffer data) {
            this.data = data;
        }

        ByteBuffer data() {
            return data;
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.CONTENT;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#END_PART}.
     */
    static final class EndPartEvent extends ParserEvent {

        private EndPartEvent() {
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.END_PART;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#END_MESSAGE}.
     */
    static final class EndMessageEvent extends ParserEvent {

        private EndMessageEvent() {
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.END_MESSAGE;
        }
    }

    /**
     * The event class for {@link EVENT_TYPE#DATA_REQUIRED}.
     */
    static final class DataRequiredEvent extends ParserEvent {

        private final boolean content;

        private DataRequiredEvent(boolean content) {
            this.content = content;
        }

        /**
         * Indicate if the required data is for the body content of a part.
         * @return {@code true} if for body content, {@code false} otherwise
         */
        boolean isContent() {
            return content;
        }

        @Override
        EVENT_TYPE type() {
            return EVENT_TYPE.DATA_REQUIRED;
        }
    }

    /**
     * Callback interface to the parser.
     */
    interface EventProcessor {

        /**
         * Process a parser event.
         * @param event generated event
         */
        void process(ParserEvent event);
    }

    /**
     * MIME Parsing exception.
     */
    static final class ParsingException extends RuntimeException {

        /**
         * Create a new exception with the specified message.
         * @param message exception message
         */
        private ParsingException(String message) {
            super(message);
        }
    }

    /**
     * Logger.
     */
    private static final Logger LOGGER
            = Logger.getLogger(MIMEParser.class.getName());

    /**
     * Encoding used to parse the header.
     */
    private static final Charset HEADER_ENCODING = Charset.forName("ISO8859-1");

    /**
     * All states.
     */
    private static enum STATE {
        START_MESSAGE,
        SKIP_PREAMBLE,
        START_PART,
        HEADERS,
        BODY,
        END_PART,
        END_MESSAGE,
        DATA_REQUIRED
    }

    /**
     * Singleton for {@link StartMessageEvent}.
     */
    private static final StartMessageEvent START_MESSAGE_EVENT =
            new StartMessageEvent();

    /**
     * Singleton for {@link StartPartEvent}.
     */
    private static final StartPartEvent START_PART_EVENT = new StartPartEvent();

    /**
     * Singleton for {@link EndHeadersEvent}.
     */
    private static final EndHeadersEvent END_HEADERS_EVENT =
            new EndHeadersEvent();

    /**
     * Singleton for {@link EndPartEvent}.
     */
    private static final EndPartEvent END_PART_EVENT = new EndPartEvent();

    /**
     * Singleton for {@link EndMessageEvent}.
     */
    private static final EndMessageEvent END_MESSAGE_EVENT =
            new EndMessageEvent();

    /**
     * The current parser state.
     */
    private STATE state = STATE.START_MESSAGE;

    /**
     * The parser state to resume to, non {@code null} when {@link #state} is
     * equal to {@link STATE#DATA_REQUIRED}.
     */
    private STATE resumeState = null;

    /**
     * Boundary as bytes.
     */
    private final byte[] bndbytes;

    /**
     * Boundary length.
     */
    private final int bl;

    /**
     * BnM algorithm: Bad Character Shift table.
     */
    private final int[] bcs = new int[128];

    /**
     * BnM algorithm : Good Suffix Shift table.
     */
    private final int[] gss;

    /**
     * Read and process body partsList until we see the terminating boundary
     * line.
     */
    private boolean done = false;

    /**
     * Beginning of the line.
     */
    private boolean bol;

    /**
     * Read-only byte array of the current byte buffer being processed.
     */
    private byte[] buf;

    /**
     * The current position in the buffer.
     */
    private int position;

    /**
     * The position of the next boundary.
     */
    private int bndStart;

    /**
     * Indicates if this parser is closed.
     */
    private boolean closed;

    /**
     * The event listener.
     */
    private final EventProcessor listener;

    /**
     * Parses the MIME content.
     */
    MIMEParser(String boundary, EventProcessor eventListener) {
        bndbytes = ("--" + boundary).getBytes();
        listener = eventListener;
        bl = bndbytes.length;
        gss = new int[bl];
        compileBoundaryPattern();
    }

    /**
     * Push new data to the parsing buffer. If the parsing buffer has non
     * processed data, it will be concatenated with the given new data.
     *
     * @param data new data add to the parsing buffer
     * @throws ParsingException if the parser state is not consistent or if
     * an error occurs during parsing
     */
    void offer(ByteBuffer data) throws ParsingException {
        if (closed) {
            throw new ParsingException("Parser is closed");
        }
        switch (state) {
            case START_MESSAGE:
                buf = Utils.toByteArray(data);
                position = 0;
                break;
            case DATA_REQUIRED:
                // resume the previous state
                state = resumeState;
                resumeState = null;
                // concat remaining data with newly pushed data
                byte[] temp = buf;
                int remaining = buf.length - position;
                buf = new byte[remaining + data.remaining()];
                System.arraycopy(temp, position, buf, 0, remaining);
                buf = Utils.copyBuffer(data, buf, remaining);
                position = 0;
                break;
            default:
                throw new ParsingException("Invalid state: " + state);
        }
        makeProgress();
    }

    /**
     * Mark this parser instance as closed. Invoking this method indicates
     * that no more data will be pushed to the parsing buffer.
     *
     * @throws ParsingException if the parser state is not
     * {@code END_MESSAGE} or {@code START_MESSAGE}
     */
    void close() throws ParsingException {
        switch (state) {
            case START_MESSAGE:
            case END_MESSAGE:
                closed = true;
                break;
            case DATA_REQUIRED:
                switch (resumeState) {
                    case SKIP_PREAMBLE:
                        throw new ParsingException("Missing start boundary");
                    case BODY:
                        throw new ParsingException("No closing MIME boundary");
                    case HEADERS:
                        throw new ParsingException("No blank line found");
                    default:
                        // do nothing
                }
                break;
            default:
                throw new ParsingException("Invalid state: " + state);
        }
    }

    /**
     * Create a "virtual" read-only of the original buffer with the specified
     * range.
     *
     * @param begin begin index
     * @param end end index
     * @return ByteBuffer
     */
    private ByteBuffer createBuffer(int begin, int end) {
        if (!(begin >= 0 && begin < buf.length)
                || !(end >= 0 && end < buf.length)
                || begin > end) {
            throw new IllegalArgumentException("invalid range");
        }
        return ByteBuffer.wrap(buf, begin, end - begin).asReadOnlyBuffer();
    }

    /**
     * Advances parsing.
     */
    private void makeProgress() throws ParsingException{

        while (true) {

            switch (state) {
                case START_MESSAGE:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.START_MESSAGE);
                    }
                    state = STATE.SKIP_PREAMBLE;
                    listener.process(START_MESSAGE_EVENT);
                    break;

                case SKIP_PREAMBLE:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.SKIP_PREAMBLE);
                    }
                    skipPreamble();
                    if (bndStart == -1) {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                    STATE.DATA_REQUIRED);
                        }
                        state = STATE.DATA_REQUIRED;
                        resumeState = STATE.SKIP_PREAMBLE;
                        listener.process(new DataRequiredEvent(false));
                        return;
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE,
                                "Skipped the preamble. position={0}",
                                buf.length);
                    }

                // fall through
                case START_PART:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.START_PART);
                    }
                    state = STATE.HEADERS;
                    listener.process(START_PART_EVENT);
                    break;

                case HEADERS:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.HEADERS);
                    }
                    String headerLine = readHeaderLine();
                    if (headerLine == null) {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                    STATE.DATA_REQUIRED);
                        }
                        state = STATE.DATA_REQUIRED;
                        resumeState = STATE.HEADERS;
                        listener.process(new DataRequiredEvent(false));
                        return;
                    }
                    if (!headerLine.isEmpty()) {
                        Hdr header = new Hdr(headerLine);
                        listener.process(new HeaderEvent(header.getName(),
                                header.getValue()));
                        break;
                    }
                    state = STATE.BODY;
                    bol = true;
                    listener.process(END_HEADERS_EVENT);
                    break;

                case BODY:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.BODY);
                    }
                    ByteBuffer content = readBody();
                    if (bndStart == -1 || content == null) {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                    STATE.DATA_REQUIRED);
                        }
                        state = STATE.DATA_REQUIRED;
                        resumeState = STATE.BODY;
                        if (content == null) {
                            listener.process(new DataRequiredEvent(true));
                            return;
                        }
                    } else {
                        bol = false;
                    }
                    listener.process(new ContentEvent(content));
                    break;

                case END_PART:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.END_PART);
                    }
                    if (done) {
                        state = STATE.END_MESSAGE;
                    } else {
                        state = STATE.START_PART;
                    }
                    listener.process(END_PART_EVENT);
                    break;

                case END_MESSAGE:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.END_MESSAGE);
                    }
                    listener.process(END_MESSAGE_EVENT);
                    return;

                case DATA_REQUIRED:
                    listener.process(new DataRequiredEvent(
                            resumeState == STATE.BODY));
                    return;

                default:
                    // nothing to do
            }
        }
    }

    /**
     * Reads the next part body content.
     *
     * @return read-only ByteBuffer, or {@code null} if more data is required
     * and no body content can be returned.
     */
    private ByteBuffer readBody() {
        // matches boundary
        bndStart = match(position, buf.length);
        if (bndStart == -1) {
            // No boundary is found
            if (position + bl + 1 < buf.length) {
                // there may be an incomplete boundary at the end of the buffer
                // return the remaining data minus the boundary length
                // so that it can be processed next iteration
                int bodyBegin = position;
                position = buf.length - (bl + 1);
                return createBuffer(bodyBegin, position);
            }
            // remaining data can be an complete boundary, force it to be
            // processed during next iteration
            return null;
        }

        // Found boundary.
        // Is it at the start of a line ?
        int bodyEnd = bndStart;
        if (bol && bndStart == position) {
            // nothing to do
        } else if (bndStart > position
                && (buf[bndStart - 1] == '\n' || buf[bndStart - 1] == '\r')) {
            --bodyEnd;
            if (buf[bndStart - 1] == '\n'
                    && bndStart > 1 && buf[bndStart - 2] == '\r') {
                --bodyEnd;
            }
        } else {
            // boundary is not at beginning of a line
            int bodyBegin = position;
            position = bodyEnd + 1;
            return createBuffer(bodyBegin, position);
        }

        // check if this is a "closing" boundary
        if (bndStart + bl + 1 < buf.length
                && buf[bndStart + bl] == '-'
                && buf[bndStart + bl + 1] == '-') {

            state = STATE.END_PART;
            done = true;
            int bodyBegin = position;
            position = bndStart + bl + 2;
            return createBuffer(bodyBegin, bodyEnd);
        }

        // Consider all the linear whitespace in boundary+whitespace+"\r\n"
        int lwsp = 0;
        for (int i = bndStart + bl
                ; i < buf.length && (buf[i] == ' ' || buf[i] == '\t')
                ; i++) {
            ++lwsp;
        }

        // Check boundary+whitespace+"\n"
        if (bndStart + bl + lwsp < buf.length
                && buf[bndStart + bl + lwsp] == '\n') {

            state = STATE.END_PART;
            int bodyBegin = position;
            position = bndStart + bl + lwsp + 1;
            return createBuffer(bodyBegin, bodyEnd);
        }

        // Check for boundary+whitespace+"\r\n"
        if (bndStart + bl + lwsp + 1 < buf.length
                && buf[bndStart + bl + lwsp] == '\r'
                && buf[bndStart + bl + lwsp + 1] == '\n') {

            state = STATE.END_PART;
            int bodyBegin = position;
            position = bndStart + bl + lwsp + 2;
            return createBuffer(bodyBegin, bodyEnd);
        }

        if (bndStart + bl + lwsp + 1 < buf.length) {
            // boundary string in a part data
            int bodyBegin = position;
            position = bodyEnd + 1;
            return createBuffer(bodyBegin, bodyEnd + 1);
        }

        // A boundary is found but it's not a "closing" boundary
        // return everything before that boundary as the "closing" characters
        // might be available next iteration
        int bodyBegin = position;
        position = bndStart;
        return createBuffer(bodyBegin, bodyEnd);
    }

    /**
     * Skips the preamble.
     */
    private void skipPreamble() {
        // matches boundary
        bndStart = match(position, buf.length);
        if (bndStart == -1) {
            // No boundary is found
            return;
        }

        // Consider all the whitespace boundary+whitespace+"\r\n"
        int lwsp = 0;
        for (int i = bndStart + bl
                ; i < buf.length && (buf[i] == ' ' || buf[i] == '\t')
                ; i++) {
            ++lwsp;
        }

        // Check for \n or \r\n
        if (bndStart + bl + lwsp < buf.length
                && (buf[bndStart + bl + lwsp] == '\n'
                || buf[bndStart + bl + lwsp] == '\r')) {

            if (buf[bndStart + bl + lwsp] == '\n') {
                position = bndStart + bl + lwsp + 1;
                return;
            } else if (bndStart + bl + lwsp + 1 < buf.length
                    && buf[bndStart + bl + lwsp + 1] == '\n') {
                position = bndStart + bl + lwsp + 2;
                return;
            }
        }
        position = bndStart + 1;
    }

    /**
     * Read the lines for a single header.
     *
     * @return a header line or an empty string if the blank line separating the
     * header from the body has been reached, or {@code null} if the there is
     * no more data in the buffer
     * @throws UnsupportedEncodingException if an error occurs while decoding
     * from the buffer
     */
    private String readHeaderLine() {
        // need more data to progress
        // need at least one blank line to read (no headers)
        if (position >= buf.length - 1) {
            return null;
        }
        int offset = position;
        int hdrLen = 0;
        int lwsp = 0;
        for (; offset + hdrLen < buf.length; hdrLen++) {
            if (buf[offset + hdrLen] == '\n') {
                lwsp += 1;
                break;
            }
            if (offset + hdrLen + 1 >= buf.length) {
                // No more data in the buffer
                return null;
            }
            if (buf[offset + hdrLen] == '\r'
                    && buf[offset + hdrLen + 1] == '\n') {
                lwsp += 2;
                break;
            }
        }
        position = offset + hdrLen + lwsp;
        if (hdrLen == 0){
            return "";
        }
        return new String(buf, offset, hdrLen, HEADER_ENCODING);
    }

    /**
     * Boyer-Moore search method.
     * Copied from {@link java.util.regex.Pattern.java}
     *
     * Pre calculates arrays needed to generate the bad character shift and the
     * good suffix shift. Only the last seven bits are used to see if chars
     * match; This keeps the tables small and covers the heavily used ASCII
     * range, but occasionally results in an aliased match for the bad character
     * shift.
     */
    private void compileBoundaryPattern() {
        int i, j;

        // Precalculate part of the bad character shift
        // It is a table for where in the pattern each
        // lower 7-bit value occurs
        for (i = 0; i < bndbytes.length; i++) {
            bcs[bndbytes[i] & 0x7F] = i + 1;
        }

        // Precalculate the good suffix shift
        // i is the shift amount being considered
        NEXT:
        for (i = bndbytes.length; i > 0; i--) {
            // j is the beginning index of suffix being considered
            for (j = bndbytes.length - 1; j >= i; j--) {
                // Testing for good suffix
                if (bndbytes[j] == bndbytes[j - i]) {
                    // src[j..len] is a good suffix
                    gss[j - 1] = i;
                } else {
                    // No match. The array has already been
                    // filled up with correct values before.
                    continue NEXT;
                }
            }
            // This fills up the remaining of optoSft
            // any suffix can not have larger shift amount
            // then its sub-suffix. Why???
            while (j > 0) {
                gss[--j] = i;
            }
        }
        // Set the guard value because of unicode compression
        gss[bndbytes.length - 1] = 1;
    }

    /**
     * Finds the boundary in the given buffer using Boyer-Moore algorithm.
     * Copied from {@link java.util.regex.Pattern.java}
     *
     * @param off start index in buf
     * @param len number of bytes in buf
     *
     * @return -1 if there is no match or index where the match starts
     */
    private int match(int off, int len) {
        int last = len - bndbytes.length;

        // Loop over all possible match positions in text
        NEXT:
        while (off <= last) {
            // Loop over pattern from right to left
            for (int j = bndbytes.length - 1; j >= 0; j--) {
                byte ch = buf[off + j];
                if (ch != bndbytes[j]) {
                    // Shift search to the right by the maximum of the
                    // bad character shift and the good suffix shift
                    off += Math.max(j + 1 - bcs[ch & 0x7F], gss[j]);
                    continue NEXT;
                }
            }
            // Entire pattern matched starting at off
            return off;
        }
        return -1;
    }

    /**
     * A private utility class to represent an individual header.
     */
    private static final class Hdr {

        /**
         * The trimmed name of this header.
         */
        String name;

        /**
         * The entire header "line".
         */
        String line;

        /**
         * Constructor that takes a line and splits out the header name.
         */
        Hdr(String l) {
            int i = l.indexOf(':');
            if (i < 0) {
                // should never happen
                name = l.trim();
            } else {
                name = l.substring(0, i).trim();
            }
            line = l;
        }

        /**
         * Return the "name" part of the header line.
         */
        public String getName() {
            return name;
        }

        /**
         * Return the "value" part of the header line.
         */
        public String getValue() {
            int i = line.indexOf(':');
            if (i < 0) {
                return line;
            }

            int j;
            // skip whitespace after ':'
            for (j = i + 1; j < line.length(); j++) {
                char c = line.charAt(j);
                if (!(c == ' ' || c == '\t')) {
                    break;
                }
            }
            return line.substring(j);
        }
    }
}
