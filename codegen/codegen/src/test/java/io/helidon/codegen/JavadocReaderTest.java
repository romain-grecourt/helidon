/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.codegen;

import java.util.List;
import java.util.Map;

import io.helidon.codegen.JavadocTree.AttrValue;
import io.helidon.codegen.JavadocTree.BlockTag;
import io.helidon.codegen.JavadocTree.Cdata;
import io.helidon.codegen.JavadocTree.Comment;
import io.helidon.codegen.JavadocTree.Doctype;
import io.helidon.codegen.JavadocTree.Document;
import io.helidon.codegen.JavadocTree.EltStart;
import io.helidon.codegen.JavadocTree.EltClose;
import io.helidon.codegen.JavadocTree.InlineTag;
import io.helidon.codegen.JavadocTree.Text;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.helidon.codegen.JavadocTree.AttrValue.EMPTY;
import static io.helidon.codegen.JavadocTree.AttrValue.Kind.DOUBLE;
import static io.helidon.codegen.JavadocTree.AttrValue.Kind.SINGLE;
import static io.helidon.codegen.JavadocTree.AttrValue.Kind.UNQUOTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class JavadocReaderTest {

    @Test
    void testEmpty() {
        var document = read("");
        assertThat(document.firstSentence(), is(empty()));
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testVoidElement() {
        var document = read("<br>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("br")));
    }

    @Test
    void testSelfClosedVoidElement() {
        var document = read("<br/>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("br", true, Map.of())));
    }

    @Test
    void testElement() {
        var document = read("<div></div>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div"),
                new EltClose("div")));
    }

    @Test
    void testElements() {
        var document = read("<div></div><p></p>");
        assertThat(document.firstSentence(), contains(
                new EltStart("div"),
                new EltClose("div")));
        assertThat(document.body(), contains(
                new EltStart("p"),
                new EltClose("p")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testNestedElements() {
        var document = read("<div><table></table></div><p></p>");
        assertThat(document.firstSentence(), contains(
                new EltStart("div"),
                new EltStart("table"),
                new EltClose("table"),
                new EltClose("div")));
        assertThat(document.body(), contains(
                new EltStart("p"),
                new EltClose("p")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testText() {
        var document = read("some text");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new Text("some text")));
    }

    @Test
    void testElementText() {
        var document = read("<p>='\"</p>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("p"),
                new Text("='\""),
                new EltClose("p")));
    }

    @Test
    void testUnquotedAttribute() {
        var document = read("<div foo=bar>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div", false, Map.of("foo", new AttrValue("bar", UNQUOTED)))));
    }

    @Test
    void testBooleanAttributes() {
        var document = read("<div foo bar>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div", false, Map.of(
                        "foo", EMPTY,
                        "bar", EMPTY))));
    }

    @Test
    void testDoubleQuotedAttribute() {
        var document = read("<div foo=\"bar\">");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div", false, Map.of("foo", new AttrValue("bar", DOUBLE)))));
    }

    @Test
    void testSingleQuotedAttribute() {
        var document = read("<div foo='bar'>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div", false, Map.of("foo", new AttrValue("bar", SINGLE)))));
    }

    @Test
    void testDoctype() {
        var document = read("<!DOCTYPE html SYSTEM \"about:legacy-compat\">");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new Doctype("html SYSTEM \"about:legacy-compat\"")));
    }

    @Test
    void testCdata() {
        var document = read("""
                <![CDATA[
                foo
                bar
                ]]>
                """);
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new Cdata("\nfoo\nbar\n")));
    }

    @Test
    void testComment() {
        var document = read("""
                <!--
                foo
                bar
                -->
                """);
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new Comment("\nfoo\nbar\n")));
    }

    @Test
    void testMixedAttributes() {
        var document = read("<div attr1 attr2=\"value2\" attr3='value3'>some-text</div>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div", false, Map.of(
                        "attr1", EMPTY,
                        "attr2", new AttrValue("value2", DOUBLE),
                        "attr3", new AttrValue("value3", SINGLE))),
                new Text("some-text"),
                new EltClose("div")));
    }

    @Test
    void testBooleanAttribute() {
        var document = read("<div attr>some-text</div>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div", false, Map.of("attr", EMPTY)),
                new Text("some-text"),
                new EltClose("div")));
    }

    @Test
    void testSelfCloseBooleanAttribute() {
        var document = read("<div attr/>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new EltStart("div", true, Map.of("attr", EMPTY))));
    }

    @Test
    void testVisit() {
        var document = read("<p><b>some-text</b></p>");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        var sb = new StringBuilder();
        for (var node : document.firstSentence()) {
            switch (node) {
                case EltStart n -> {
                    sb.append("<");
                    if (n.selfClosing()) {
                        sb.append("/");
                    }
                    sb.append(n.name());
                    sb.append(">");
                }
                case Text n -> sb.append(n.value());
                case EltClose n -> sb.append("</").append(n.name()).append(">");
                default -> {
                }
            }
        }
        assertThat(sb.toString(), is("<p><b>some-text</b></p>"));
    }

    @Test
    void testUnclosed() {
        var document = read("<div><p>some-text</div>");
        assertThat(document.firstSentence(), contains(
                new EltStart("div")));
        assertThat(document.body(), contains(
                new EltStart("p"),
                new Text("some-text"),
                new EltClose("div")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testInlineTag() {
        var document = read("{@code}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new InlineTag("code", "")));
    }

    @Test
    void testInlineTagBody() {
        var document = read("{@code foo}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new InlineTag("code", "foo")));
    }

    @Test
    void testNestedInlineTag() {
        var document = read("{@code {@code foo}}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new InlineTag("code", "{@code foo}")));
    }

    @Test
    void testInlineWithNestedCurly() {
        var document = read("{@code {}}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new InlineTag("code", "{}")));
    }

    @Test
    void testInlineLeadingCurly() {
        var document = read("{{@code {}}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new Text("{"),
                new InlineTag("code", "{}")));
    }

    @Test
    void testInlineTrailingCurly() {
        var document = read("{@code {}}}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new InlineTag("code", "{}"),
                new Text("}")));
    }

    @Test
    void testInlineLink() {
        var document = read("{@link StuffMakerImpl#makeStuff() my-link}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new InlineTag("link", "StuffMakerImpl#makeStuff() my-link")));
    }

    @Test
    void testBlockTagBody() {
        var document = read("@param test\n");
        assertThat(document.firstSentence(), is(empty()));
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), contains(
                new BlockTag("param", List.of(new Text("test")))));
    }

    @Test
    void testBlockTagBodyWithLeadingSpaces() {
        var document = read("@param   test\n");
        assertThat(document.firstSentence(), is(empty()));
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), contains(
                new BlockTag("param", List.of(new Text("test")))));
    }

    @Test
    void testBlockTagWithoutBody() {
        var document = read("@param");
        assertThat(document.firstSentence(), is(empty()));
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), contains(
                new BlockTag("param", List.of())));
    }

    @Test
    void testTwoBlockTags() {
        var document = read("@param p1 param1\n@param p2 param2");
        assertThat(document.firstSentence(), is(empty()));
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), contains(
                new BlockTag("param", List.of(new Text("p1 param1"))),
                new BlockTag("param", List.of(new Text("p2 param2")))));
    }

    @Test
    void testBlockTagWithNestedInlineTags() {
        var document = read("@deprecated use {@link com.acme.Foo} instead");
        assertThat(document.firstSentence(), is(empty()));
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), contains(
                new BlockTag("deprecated", List.of(
                        new Text("use "),
                        new InlineTag("link", "com.acme.Foo"),
                        new Text(" instead")))));
    }

    @Test
    void testSingleAt() {
        var document = read("@");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new Text("@")));
    }

    @Test
    void testSingleCurlyAt() {
        var document = read("{@");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), contains(
                new Text("{@")));
    }

    @Test
    void testEscapedBlockTag() {
        var document = read("@@param");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), Matchers.contains(
                new JavadocTree.Escape("@"),
                new Text("param")));
    }

    @Test
    void testTextWithEscapedBlockTag() {
        var document = read("some text\n @@param");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), Matchers.contains(
                new Text("some text\n "),
                new JavadocTree.Escape("@"),
                new Text("param")));
    }

    @Test
    void testEscapedInlineTag() {
        var document = read("{@@code}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), Matchers.contains(
                new Text("{"),
                new JavadocTree.Escape("@"),
                new Text("code}")));
    }

    @Test
    void testTextWithEscapedInlineTag() {
        var document = read("some text {@@code}");
        assertThat(document.body(), is(empty()));
        assertThat(document.blockTags(), is(empty()));
        assertThat(document.firstSentence(), Matchers.contains(
                new Text("some text {"),
                new JavadocTree.Escape("@"),
                new Text("code}")));
    }

    @Test
    void testFirstSentence() {
        var document = read("First sentence. Body sentence.");
        assertThat(document.firstSentence(), contains(
                new Text("First sentence.")));
        assertThat(document.body(), contains(
                new Text("Body sentence.")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testFirstSentenceWithDottedCode() {
        var document = read("First {@code 1.2.3} sentence. Body sentence.");
        assertThat(document.firstSentence(), contains(
                new Text("First "),
                new InlineTag("code", "1.2.3"),
                new Text(" sentence.")));
        assertThat(document.body(), contains(
                new Text("Body sentence.")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testFirstSentenceWithDottedHtmlCode() {
        var document = read("First <code>1.2.3</code> sentence. Body sentence.");
        assertThat(document.firstSentence(), contains(
                new Text("First "),
                new EltStart("code"),
                new Text("1.2.3"),
                new EltClose("code"),
                new Text(" sentence.")));
        assertThat(document.body(), contains(
                new Text("Body sentence.")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testFirstSentenceWithLink() {
        var document = read("Wrapper for {@link com.acme.Service} class. Body sentence.");
        assertThat(document.firstSentence(), contains(
                new Text("Wrapper for "),
                new InlineTag("link", "com.acme.Service"),
                new Text(" class.")));
        assertThat(document.body(), contains(
                new Text("Body sentence.")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testFirstSentenceWithHtmlLink() {
        var document = read("See <a href=\"https://acme.com\">acme.com</a>. Body sentence.");
        assertThat(document.firstSentence(), contains(
                new Text("See "),
                new EltStart("a", false, Map.of("href", new AttrValue("https://acme.com", DOUBLE))),
                new Text("acme.com"),
                new EltClose("a"),
                new Text(".")));
        assertThat(document.body(), contains(
                new Text("Body sentence.")));
        assertThat(document.blockTags(), is(empty()));
    }

    @Test
    void testSummaryBreak() {
        var document = read("A class with summary.\n {@summary The summary}");
        assertThat(document.firstSentence(), contains(
                new Text("A class with summary.")));
        assertThat(document.body(), contains(
                new InlineTag("summary", "The summary")));
        assertThat(document.blockTags(), is(empty()));
    }

    private static Document read(String input) {
        var reader = JavadocReader.create(input);
        return reader.read();
    }
}
