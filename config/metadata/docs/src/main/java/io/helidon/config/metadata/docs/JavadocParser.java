/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.config.metadata.docs;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Javadoc parser.
 */
class JavadocParser implements Iterator<JavadocParser.Event> {

    /**
     * Event.
     */
    sealed interface Event permits Event.Literal,
                                   Event.Text,
                                   Event.Comment,
                                   Event.Cdata,
                                   Event.Doctype,
                                   Event.EltStart,
                                   Event.AttrsEnd,
                                   Event.EltClose,
                                   Event.AttrName,
                                   Event.AttrValue {

        /**
         * {@link EltClose} constant.
         */
        Event ELT_CLOSE = new EltClose();

        /**
         * {@link AttrsEnd} constant.
         */
        // TODO remove, not needed
        Event ATTRS_END = new AttrsEnd();

        /**
         * Text.
         *
         * @param value value
         */
        record Text(String value) implements Event {
        }

        /**
         * HTML literal (element name, attribute value).
         *
         * @param value value
         */
        record Literal(String value) implements Event {
        }

        /**
         * HTML comment.
         *
         * @param value value
         */
        record Comment(String value) implements Event {
        }

        /**
         * HTML CDATA.
         *
         * @param value value
         */
        record Cdata(String value) implements Event {
        }

        /**
         * HTML doctype.
         *
         * @param value value
         */
        record Doctype(String value) implements Event {
            /**
             * Create a new instance.
             *
             * @param value value
             */
            public Doctype(String value) {
                this.value = value.trim();
            }
        }

        /**
         * HTML attribute name.
         *
         * @param value value
         */
        record AttrName(String value) implements Event {
        }

        /**
         * HTML attribute.
         *
         * @param value value
         */
        record AttrValue(String value) implements Event {
        }

        /**
         * HTML attributes.
         */
        record AttrsEnd() implements Event {
        }

        /**
         * HTML element start (before attributes).
         *
         * @param value value
         */
        record EltStart(String value) implements Event {
        }

        /**
         * HTML element close.
         */
        record EltClose() implements Event {
        }
    }

    private enum Token {
        WHITESPACE(JavadocParser::consumeWhiteSpace),
        DOCTYPE("<!DOCTYPE"),
        CDATA("<![CDATA["),
        COMMENT("<!--"),
        CLOSE("</"),
        SELF_CLOSE("/>"),
        LOWER_THAN('<'),
        GREATER_THAN('>'),
        SINGLE_QUOTE("='"),
        DOUBLE_QUOTE("=\""),
        EQUAL('=');

        private final Function<JavadocParser, Boolean> reader;

        Token(Function<JavadocParser, Boolean> function) {
            reader = function;
        }

        Token(char ch) {
            reader = t -> t.consumeChar(ch);
        }

        Token(String str) {
            reader = t -> t.consumeString(str);
        }
    }

    enum State {
        TOKEN(null, null, 0),
        TEXT(p -> p.readChar('<'), Event.Text::new, 0),
        DOCTYPE(p -> p.readChar('>'), Event.Doctype::new, 1),
        SINGLE_QUOTE(p -> p.readChar('\''), Event.AttrValue::new, 1),
        DOUBLE_QUOTE(p -> p.readChar('"'), Event.AttrValue::new, 1),
        CDATA(p -> p.readString("]]>"), Event.Cdata::new, 3),
        COMMENT(p -> p.readString("-->"), Event.Comment::new, 3),
        ELEMENT(JavadocParser::readEndElement, Event.EltStart::new, 0),
        ATTRIBUTE(JavadocParser::readEndAttribute, Event.AttrName::new, 0),
        UNQUOTED(JavadocParser::readEndUnquoted, Event.AttrValue::new, 0),
        CLOSE(JavadocParser::readEndElement, s -> Event.ELT_CLOSE, 1);

        private final Predicate<JavadocParser> predicate;
        private final Function<String, Event> function;
        private final int offset;

        State(Predicate<JavadocParser> predicate, Function<String, Event> function, int offset) {
            this.predicate = predicate;
            this.function = function;
            this.offset = offset;
        }
    }

    private final char[] buf;
    private int position;
    private int lastPosition;
    private int valuePosition;
    private int lineNo = 1;
    private int charNo = 0;
    private State lastState = State.TOKEN;
    private State state = State.TOKEN;
    private Event event;

    /**
     * Create a new instance.
     *
     * @param str content to parse
     */
    JavadocParser(String str) {
        buf = str.toCharArray();
    }

    @Override
    public boolean hasNext() {
        while (position < buf.length && event == null) {
            char c = buf[position];
            if (c == '\n') {
                lineNo++;
                charNo = 1;
            }
            lastPosition = position;
            if (state == State.TOKEN) {
                boolean foundToken = false;
                for (var token : Token.values()) {
                    if (token.reader.apply(this)) {
                        valuePosition = position;
                        switch (token) {
                            case LOWER_THAN -> nextState(State.ELEMENT);
                            case GREATER_THAN -> {
                                event = Event.ATTRS_END;
                                nextState(State.TEXT);
                            }
                            case SELF_CLOSE -> {
                                event = Event.ELT_CLOSE;
                                nextState(State.TEXT);
                            }
                            case CLOSE -> nextState(State.CLOSE);
                            case EQUAL -> nextState(State.UNQUOTED);
                            case SINGLE_QUOTE -> nextState(State.SINGLE_QUOTE);
                            case DOUBLE_QUOTE -> nextState(State.DOUBLE_QUOTE);
                            case DOCTYPE -> nextState(State.DOCTYPE);
                            case COMMENT -> nextState(State.COMMENT);
                            case CDATA -> nextState(State.CDATA);
                            default -> {
                                // ignore
                            }
                        }
                        foundToken = true;
                        break;
                    }
                }
                if (!foundToken) {
                    valuePosition = position;
                    switch (lastState) {
                        case ELEMENT,
                             ATTRIBUTE,
                             SINGLE_QUOTE,
                             DOUBLE_QUOTE -> nextState(State.ATTRIBUTE);
                        default -> nextState(State.TEXT);
                    }
                }
            } else {
                if (state.predicate.test(this)) {
                    position += state.offset;
                    if (state != State.TEXT || lastPosition > valuePosition) {
                        event = state.function.apply(value());
                    }
                    nextState(State.TOKEN);
                } else if (++position >= buf.length) {
                    lastPosition = position;
                    event = state.function.apply(value());
                }
            }
            charNo += (position - lastPosition);
        }
        return event != null;
    }

    @Override
    public Event next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Event next = event;
        event = null;
        return next;
    }

    /**
     * Describe the current parser position (line and column).
     *
     * @return string
     */
    String cursor() {
        return "line: %d, col: %d".formatted(lineNo, charNo);
    }

    /**
     * Get the current event without consuming it.
     *
     * @return event, never {@code null}
     * @throws java.util.NoSuchElementException if there is no event
     */
    Event peek() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return event;
    }

    /**
     * Discard the current event.
     */
    void skip() {
        event = null;
    }

    private String value() {
        return String.valueOf(buf, valuePosition, lastPosition - valuePosition);
    }

    private void nextState(State nextState) {
        lastState = state;
        state = nextState;
    }

    private boolean consumeWhiteSpace() {
        char c = buf[position];
        if (Character.isWhitespace(c)) {
            position++;
            return true;
        }
        return false;
    }

    private boolean consumeChar(char expected) {
        char c = buf[position];
        if (c == expected) {
            position++;
            return true;
        }
        return false;
    }

    private boolean consumeString(String str) {
        if (readString(str)) {
            position += str.length();
            return true;
        }
        return false;
    }

    private boolean readChar(char c) {
        return buf[position] == c;
    }

    private boolean readString(String str) {
        if (position + str.length() < buf.length + 1) {
            return str.equals(String.valueOf(buf, position, str.length()));
        }
        return false;
    }

    private boolean readEndAttribute() {
        char c = buf[position];
        return switch (c) {
            case '"', '\'', '>', '/', '=' -> true;
            default -> Character.isWhitespace(c);
        };
    }

    private boolean readEndUnquoted() {
        char c = buf[position];
        return switch (c) {
            case '"', '\'', '=', '/', '<', '>', '`' -> true;
            default -> Character.isWhitespace(c);
        };
    }

    private boolean readEndElement() {
        char c = buf[position];
        return c == '>'
               || c == '/'
               || Character.isWhitespace(c);
    }
}
