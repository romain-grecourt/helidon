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

import java.util.ArrayList;
import java.util.List;

import io.helidon.config.metadata.docs.JavadocParser.Event;

import org.junit.jupiter.api.Test;

import static io.helidon.config.metadata.docs.JavadocParser.Event.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class JavadocParserTest {

    // TODO:
    //  - javadoc escapes
    //  - optional close elements (<li>, <p> etc.)
    //  - javadoc nodes

    @Test
    void testEmpty() {
        List<Event> symbols = parse("");
        assertThat(symbols, is(empty()));
    }

    @Test
    void testVoidElement() {
        List<Event> symbols = parse("<br>");
        assertThat(symbols, contains(
                new EltStart("br"),
                ATTRS_END));
    }

    @Test
    void testSelfClosedVoidElement() {
        List<Event> symbols = parse("<br/>");
        assertThat(symbols, contains(
                new EltStart("br"),
                ELT_CLOSE));
    }

    @Test
    void testSelfCloseUnquotedAttribute() {
        List<Event> symbols = parse("<button foo=bar/>");
        assertThat(symbols, contains(
                new EltStart("button"),
                new AttrName("foo"),
                new AttrValue("bar"),
                ELT_CLOSE));
    }

    @Test
    void testElement() {
        List<Event> events = parse("<div></div>");
        assertThat(events, contains(
                new EltStart("div"),
                ATTRS_END,
                ELT_CLOSE));
    }

    @Test
    void testText() {
        List<Event> events = parse("some text");
        assertThat(events, contains(new Text("some text")));
    }

    @Test
    void testElementText() {
        List<Event> events = parse("<p>='\"</p>");
        assertThat(events, contains(
                new EltStart("p"),
                ATTRS_END,
                new Text("='\""),
                ELT_CLOSE));
    }

    @Test
    void testUnquotedAttribute() {
        List<Event> events = parse("<div foo=bar>");
        assertThat(events, contains(
                new EltStart("div"),
                new AttrName("foo"),
                new AttrValue("bar"),
                ATTRS_END));
    }

    @Test
    void testUnquotedBooleanAttribute() {
        List<Event> events = parse("<input @keyup @observe/>");
        assertThat(events, contains(
                new EltStart("input"),
                new AttrName("@keyup"),
                new AttrName("@observe"),
                ELT_CLOSE));
    }

    @Test
    void testDoubleQuotedAttribute() {
        List<Event> events = parse("<div foo=\"bar\">");
        assertThat(events, contains(
                new EltStart("div"),
                new AttrName("foo"),
                new AttrValue("bar"),
                ATTRS_END));
    }

    @Test
    void testSingleQuotedAttribute() {
        List<Event> events = parse("<div foo='bar'>");
        assertThat(events, contains(
                new EltStart("div"),
                new AttrName("foo"),
                new AttrValue("bar"),
                ATTRS_END));
    }

    @Test
    void testDoctype() {
        List<Event> events = parse("<!DOCTYPE html SYSTEM \"about:legacy-compat\">");
        assertThat(events, contains(new Doctype("html SYSTEM \"about:legacy-compat\"")));
    }

    @Test
    void testCdata() {
        List<Event> events = parse("""
                                                     <![CDATA[
                                                     foo
                                                     bar
                                                     ]]>
                                                     """);
        assertThat(events, contains(new Cdata("\nfoo\nbar\n")));
    }

    @Test
    void testComment() {
        List<Event> events = parse("""
                                                     <!--
                                                     foo
                                                     bar
                                                     -->
                                                     """);
        assertThat(events, contains(new Comment("\nfoo\nbar\n")));
    }

    static List<Event> parse(String str) {
        var parser = new JavadocParser(str);
        var events = new ArrayList<Event>();
        while (parser.hasNext()) {
            var event = parser.next();
            events.add(event);
        }
        return events;
    }
}
