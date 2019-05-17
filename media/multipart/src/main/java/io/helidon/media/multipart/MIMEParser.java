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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * MIME messages push parser.
 */
class MIMEParser implements Iterable<MIMEEvent> {

    /**
     * Logger.
     */
    private static final Logger LOGGER =
            Logger.getLogger(MIMEParser.class.getName());

    /**
     * Encoding used to parse the header.
     */
    private static final String HEADER_ENCODING = "ISO8859-1";

    /**
     * All states.
     */
    private enum STATE {
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
     * The current parser state.
     */
    private STATE state = STATE.START_MESSAGE;

    /**
     * The parser state to resume.
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
     * Parses the MIME content.
     */
    MIMEParser(String boundary) {
        bndbytes = ("--" + boundary).getBytes();
        bl = bndbytes.length;
        gss = new int[bl];
        compileBoundaryPattern();
    }

    /**
     * Push new data to the parsing buffer. If the parsing buffer has non
     * processed data, it will be concatenated with the given new data.
     *
     * @param data new data add to the parsing buffer
     * @throws MIMEParsingException if the parser state is not
     * {@code START_MESSAGE} or {@code DATA_REQUIRED}
     */
    void offer(ByteBuffer data) {
        if (closed) {
            throw new MIMEParsingException("Parser is closed");
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
                throw new MIMEParsingException("Parser not drained");
        }
    }

    /**
     * Mark this parser instance as closed. Invoking this method indicates
     * that no more data will be pushed to the parsing buffer.
     *
     * @throws MIMEParsingException if the parser state is not
     * {@code END_MESSAGE} or {@code START_MESSAGE}
     */
    void close() {
        if (state == STATE.START_MESSAGE || state == STATE.END_MESSAGE) {
            closed = true;
            return;
        }
        if (state == STATE.DATA_REQUIRED) {
            if (resumeState == STATE.SKIP_PREAMBLE) {
                throw new MIMEParsingException("Missing start boundary");
            }
            if (resumeState == STATE.BODY) {
                throw new MIMEParsingException("No closing MIME boundary");
            }
            if (resumeState == STATE.HEADERS) {
                throw new MIMEParsingException("No blank line found");
            }
        }
        throw new MIMEParsingException("Invalid state: " + state);
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
     * Returns iterator for the parsing events. Use the iterator to advance the
     * parsing.
     *
     * @return iterator for parsing events
     */
    @Override
    public Iterator<MIMEEvent> iterator() {
        return new MIMEEventIterator();
    }

    private class MIMEEventIterator implements Iterator<MIMEEvent> {

        @Override
        public boolean hasNext() {
            return !closed && !(state == STATE.END_MESSAGE
                    ||  state == STATE.DATA_REQUIRED);
        }

        @Override
        public MIMEEvent next() {

            if (state == STATE.END_MESSAGE || closed) {
                throw new NoSuchElementException();
            }

            switch (state) {
                case START_MESSAGE:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.START_MESSAGE);
                    }
                    state = STATE.SKIP_PREAMBLE;
                    return MIMEEvent.START_MESSAGE;

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
                        return MIMEEvent.DATA_REQUIRED;
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
                    return MIMEEvent.START_PART;

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
                        return MIMEEvent.DATA_REQUIRED;
                    }
                    if (!headerLine.isEmpty()){
                        Hdr header = new Hdr(headerLine);
                        return new MIMEEvent.Header(header.getName(),
                                header.getValue());
                    }
                    state = STATE.BODY;
                    bol = true;
                    return MIMEEvent.END_HEADERS;

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
                            return MIMEEvent.DATA_REQUIRED;
                        }
                    } else {
                        bol = false;
                    }
                    return new MIMEEvent.Content(content);

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
                    return MIMEEvent.END_PART;

                case END_MESSAGE:
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, "MIMEParser state={0}",
                                STATE.END_MESSAGE);
                    }
                    return MIMEEvent.END_MESSAGE;

                case DATA_REQUIRED:
                    throw new MIMEParsingException("More data required");

                default:
                    throw new MIMEParsingException("Unknown state = " + state);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
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

        // Consider all the whitespace in boundary+whitespace+"\r\n"
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
     *  no more data in the buffer
     * @throws MIMEParsingException if an error occurs while decoding
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
        try {
            return new String(buf, offset, hdrLen, HEADER_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            throw new MIMEParsingException("Error reading header line", ex);
        }
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
         * The entire RFC822 header "line".
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
                if (!(c == ' ' || c == '\t' || c == '\r' || c == '\n')) {
                    break;
                }
            }
            return line.substring(j);
        }
    }
}
