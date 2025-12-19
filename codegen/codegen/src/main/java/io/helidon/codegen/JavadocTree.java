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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Javadoc element.
 * <p>
 * <b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or deletion without notice.</b>
 * </p>
 */
public sealed interface JavadocTree permits JavadocTree.EltStart,
                                            JavadocTree.InlineTag,
                                            JavadocTree.BlockTag,
                                            JavadocTree.EltClose,
                                            JavadocTree.Text,
                                            JavadocTree.Escape,
                                            JavadocTree.Comment,
                                            JavadocTree.Cdata,
                                            JavadocTree.Doctype {

    /**
     * Root node.
     *
     * @param firstSentence the first sentence of a documentation comment.
     * @param body          body of a documentation comment, appearing after the first sentence, and before any block tags
     * @param blockTags     block tags of a documentation comment
     */
    record Document(List<JavadocTree> firstSentence, List<JavadocTree> body, List<BlockTag> blockTags) {

        /**
         * Get the full body.
         *
         * @return elements
         */
        public List<JavadocTree> fullBody() {
            var elements = new ArrayList<>(firstSentence);
            elements.addAll(body);
            return elements;
        }
    }

    /**
     * HTML start element.
     *
     * @param name        name
     * @param selfClosing self-closing
     * @param attributes  attributes
     */
    record EltStart(String name, boolean selfClosing, Map<String, AttrValue> attributes) implements JavadocTree {
        EltStart(String name) {
            this(name, false, Map.of());
        }
    }

    /**
     * HTML attribute value.
     *
     * @param value value
     * @param kind  kind
     */
    record AttrValue(String value, AttrValue.Kind kind) implements JavadocParser.Event {

        /**
         * Constant for the empty attribute value.
         */
        public static final AttrValue EMPTY = new AttrValue("", AttrValue.Kind.EMPTY);

        /**
         * Attribute value kind.
         */
        public enum Kind {
            /**
             * Empty.
             */
            EMPTY,
            /**
             * Single quoted.
             */
            SINGLE,
            /**
             * Double quoted.
             */
            DOUBLE,
            /**
             * Unquoted.
             */
            UNQUOTED
        }
    }

    /**
     * Text.
     *
     * @param value value
     */
    record Text(String value) implements JavadocParser.Event, JavadocTree {
    }

    /**
     * Escape.
     *
     * @param value value
     */
    record Escape(String value) implements JavadocParser.Event, JavadocTree {
    }

    /**
     * HTML comment.
     *
     * @param value value
     */
    record Comment(String value) implements JavadocParser.Event, JavadocTree {
    }

    /**
     * HTML CDATA.
     *
     * @param value value
     */
    record Cdata(String value) implements JavadocParser.Event, JavadocTree {
    }

    /**
     * HTML doctype.
     *
     * @param value value
     */
    record Doctype(String value) implements JavadocParser.Event, JavadocTree {
    }

    /**
     * HTML element close.
     *
     * @param name name
     */
    record EltClose(String name) implements JavadocParser.Event, JavadocTree {
    }

    /**
     * Inline tag.
     *
     * @param tag  tag
     * @param body body
     */
    record InlineTag(String tag, String body) implements JavadocTree {
    }

    /**
     * Block tag.
     *
     * @param tag  tag
     * @param body body
     */
    record BlockTag(String tag, List<JavadocTree> body) implements JavadocTree {
    }
}
