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
import java.util.Objects;

import io.helidon.config.metadata.docs.JavadocElement.Node;
import io.helidon.config.metadata.docs.JavadocElement.TextNode;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

class JavadocElementTest {

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
        assertThat(nodes, contains(isNode("br")));
    }

    @Test
    void testSelfClosedVoidElement() {
        var root = JavadocElement.parse("<br/>");
        var nodes = root.traverse();
        assertThat(nodes, contains(isNode("br")));
    }

    @Test
    void testElement() {
        var root = JavadocElement.parse("<div></div>");
        var nodes = root.traverse();
        assertThat(nodes, contains(isNode("div")));
    }

    @Test
    void testElements() {
        var root = JavadocElement.parse("<div></div><p></p>");
        var nodes = root.traverse();
        assertThat(nodes, contains(List.of(
                isNode("div"),
                isNode("p"))));
    }

    @Test
    void testNestedElements() {
        var root = JavadocElement.parse("<div><table></table></div><p></p>");
        var nodes = root.traverse();
        assertThat(nodes, contains(List.of(
                isNode("div"),
                isNode("table"),
                isNode("p"))));
    }

    @Test
    void testText() {
        var root = JavadocElement.parse("some text");
        var nodes = root.traverse();
        assertThat(nodes, contains(isText("some text")));
    }

    @Test
    void testElementText() {
        var root = JavadocElement.parse("<p>='\"</p>");
        var nodes = root.traverse();
        assertThat(nodes, contains(List.of(
                isNode("p"),
                isText("='\""))));
    }

    @Test
    void testUnquotedAttribute() {
        var root = JavadocElement.parse("<div foo=bar>");
        var nodes = root.traverse();
        assertThat(nodes, contains(
                isNode("div", Map.of("foo", "bar"))));
    }

    @Test
    void testUnquotedBooleanAttribute() {
        var root = JavadocElement.parse("<div foo=bar>");
        var nodes = root.traverse();
        assertThat(nodes, contains(isNode("div", Map.of("foo", "bar"))));
    }

    @Test
    void testDoubleQuotedAttribute() {
        var root = JavadocElement.parse("<div foo=\"bar\">");
        var nodes = root.traverse();
        assertThat(nodes, contains(isNode("div", Map.of("foo", "bar"))));
    }

    @Test
    void testSingleQuotedAttribute() {
        var root = JavadocElement.parse("<div foo='bar'>");
        var nodes = root.traverse();
        assertThat(nodes, contains(isNode("div", Map.of("foo", "bar"))));
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
        assertThat(nodes, contains(isText("\nfoo\nbar\n")));
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
        assertThat(nodes, is(List.of()));
    }

    @Test
    void testMixedAttributes() {
        var root = JavadocElement.parse("<div attr1 attr2=\"value2\" attr3='value3'>some-text</>");
        var nodes = root.traverse();
        assertThat(nodes, contains(List.of(
                isNode("div", Map.of(
                        "attr1", "",
                        "attr2", "value2",
                        "attr3", "value3")),
                isText("some-text"))));
    }

    @Test
    void testBooleanAttribute() {
        var root = JavadocElement.parse("<div attr>some-text</>");
        var nodes = root.traverse();
        assertThat(nodes, contains(List.of(
                isNode("div", Map.of("attr", "")),
                isText("some-text"))));
    }

    @Test
    void testSelfCloseBooleanAttribute() {
        var root = JavadocElement.parse("<div attr/>");
        var nodes = root.traverse();
        assertThat(nodes, contains(
                isNode("div", Map.of("attr", ""))));
    }

    @Test
    void testVisit() {
        var root = JavadocElement.parse("<p><b>some-text</b></p>");
        var sb = new StringBuilder();
        root.visit(new JavadocElement.Visitor() {
            @Override
            public void visitElement(JavadocElement elt) {
                if (elt instanceof Node n) {
                    sb.append("<").append(n.name()).append(">");
                } else if (elt instanceof TextNode n) {
                    sb.append(n.value());
                }
            }

            @Override
            public void postVisitElement(JavadocElement elt) {
                if (elt instanceof Node n) {
                    sb.append("</").append(n.name()).append(">");
                }
            }
        });
        assertThat(sb.toString(), is("<p><b>some-text</b></p>"));
    }

    static TypeSafeMatcher<JavadocElement> isNode(String name) {
        return isNode(name, Map.of());
    }

    static TypeSafeMatcher<JavadocElement> isNode(String name, Map<String, String> attrs) {
        return new NodeMatcher(name, attrs);
    }

    static TypeSafeMatcher<JavadocElement> isText(String value) {
        return new TextNodeMatcher(value);
    }

    static class TextNodeMatcher extends TypeSafeMatcher<JavadocElement> {

        private final String value;

        TextNodeMatcher(String value) {
            this.value = value;
        }

        @Override
        protected boolean matchesSafely(JavadocElement actual) {
            if (actual instanceof TextNode n) {
                return Objects.equals(value, n.value());
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
        }
    }

    static class NodeMatcher extends TypeSafeMatcher<JavadocElement> {

        private final String name;
        private final Map<String, String> attributes;

        NodeMatcher(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        @Override
        protected boolean matchesSafely(JavadocElement actual) {
            if (actual instanceof Node n) {
                return Objects.equals(name, n.name())
                       && Objects.equals(attributes, n.attributes());
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
        }
    }
}
