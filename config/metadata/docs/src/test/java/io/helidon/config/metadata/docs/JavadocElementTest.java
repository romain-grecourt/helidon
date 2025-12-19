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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

class JavadocElementTest {

    // TODO
    //  - text as nodes
    //  - toHtml()
    //  - optional closing element (<li>, <p> etc.)
    //  - javadoc escapes (@)
    //  - javadoc nodes (E.g. {@link) -> <javadoc:link>)

    @Test
    void testEmpty() {
        var root = JavadocElement.parse("");
        var nodes = root.traverse();
        assertThat(nodes, is(List.of()));
    }

    @Test
    void testVoidElement() {
        var root = JavadocElement.parse("<br>");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement("br")));
    }

    @Test
    void testSelfClosedVoidElement() {
        var root = JavadocElement.parse("<br/>");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement("br")));
    }

    @Test
    void testElement() {
        var root = JavadocElement.parse("<div></div>");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement("div")));
    }

    @Test
    void testElements() {
        var root = JavadocElement.parse("<div></div><p></p>");
        var nodes = root.traverse();
        assertThat(nodes, contains(
                new JavadocElement("div"),
                new JavadocElement("p")));
    }

    @Test
    void testNestedElements() {
        var root = JavadocElement.parse("<div><table></table></div><p></p>");
        var nodes = root.traverse();
        assertThat(nodes, contains(
                new JavadocElement("div"),
                new JavadocElement("table"),
                new JavadocElement( "p")));
    }

    @Test
    void testText() {
        var root = JavadocElement.parse("some text");
        var nodes = root.traverse();
        assertThat(nodes, contains(
                new JavadocElement(null, "", Map.of(), "some text")));
    }

    @Test
    void testElementText() {
        var root = JavadocElement.parse("<p>='\"</p>");
        var nodes = root.traverse();
        assertThat(nodes, contains(
                new JavadocElement("p"),
                new JavadocElement(null, "", Map.of(), "='\"")));
    }

    @Test
    void testUnquotedAttribute() {
        var root = JavadocElement.parse("<div foo=bar>");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement(null, "div", Map.of("foo", "bar"), "")));
    }

    @Test
    void testUnquotedBooleanAttribute() {
        var root = JavadocElement.parse("<div foo=bar>");
        var nodes = root.traverse();
        var attrs = Map.of(
                "foo", "",
                "bar", "");
        assertThat(nodes, contains(new JavadocElement(null, "div", attrs, "")));
    }

    @Test
    void testDoubleQuotedAttribute() {
        var root = JavadocElement.parse("<div foo=\"bar\">");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement(null, "div", Map.of("foo", "bar"), "")));
    }

    @Test
    void testSingleQuotedAttribute() {
        var root = JavadocElement.parse("<div foo='bar'>");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement(null, "div", Map.of("foo", "bar"), "")));
    }

    @Test
    void testDoctype() {
        var root = JavadocElement.parse("<!DOCTYPE html SYSTEM \"about:legacy-compat\">");
        var nodes = root.traverse();
        assertThat(nodes, is(List.of()));
    }

    @Test
    void testCdata() {
        var root = JavadocElement.parse("""
                       <![CDATA[
                       foo
                       bar
                       ]]>
                       """);
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement(null, "", Map.of(), "\nfoo\nbar\n")));
    }

    @Test
    void testComment() {
        var root = JavadocElement.parse("""
                                  <!--
                                  foo
                                  bar
                                  -->
                                  """);
        var nodes = root.traverse();
        assertThat(nodes.size(), is(0));
    }

    @Test
    void testVariableMixedAttributes() {
        var root = JavadocElement.parse("<div attr1 attr2=\"value2\" attr3='value3'>some-text</>");
        var nodes = root.traverse();
        var attrs = Map.of(
                "attr1", "",
                "attr2", "value2",
                "attr3", "value3");
        assertThat(nodes, contains(new JavadocElement(null, "div", attrs, "")));
    }

    @Test
    void testBooleanAttribute() {
        var root = JavadocElement.parse("<div attr>some-text</>");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement(null, "div", Map.of("attr", ""), "some-text")));
    }

    @Test
    void testSelfCloseBooleanAttribute() {
        var root = JavadocElement.parse("<div attr/>");
        var nodes = root.traverse();
        assertThat(nodes, contains(new JavadocElement(null, "div", Map.of("attr", ""), "")));
    }
}
