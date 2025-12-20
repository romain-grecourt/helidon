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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Javadoc element.
 */
public sealed interface JavadocElement permits JavadocElement.Node,
                                               JavadocElement.RootNode,
                                               JavadocElement.TextNode {

    /**
     * Visitor.
     */
    interface Visitor {
        /**
         * Visit (entering) an element.
         *
         * @param elt element
         */
        void visitElement(JavadocElement elt);

        /**
         * Post-visit (leaving) an element.
         *
         * @param elt element
         */
        default void postVisitElement(JavadocElement elt) {
        }
    }

    /**
     * Parent node.
     *
     * @return parent, never {@code null}
     */
    JavadocElement parent();

    /**
     * Children.
     *
     * @return children, never {@code null}
     */
    List<JavadocElement> children();

    // TODO:
    //  - optional closing element (<li>, <p> etc.)
    //  - javadoc escapes (@)
    //  - javadoc nodes (E.g. {@link) -> <javadoc:link>)

    /**
     * Visit this element.
     *
     * @param visitor visitor
     */
    default void visit(Visitor visitor) {
        Deque<JavadocElement> stack = new ArrayDeque<>(children());
        var parent = this;
        while (!stack.isEmpty()) {
            var elt = stack.peek();
            if (elt == parent) {
                visitor.postVisitElement(elt);
                parent = elt.parent();
                stack.pop();
            } else {
                visitor.visitElement(elt);
                var children = elt.children();
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
                if (parent != elt.parent()) {
                    throw new IllegalStateException("Parent mismatch");
                }
                parent = elt;
            }
        }
    }

    /**
     * Traverse this node (depth-first).
     *
     * @return list of nodes
     */
    default List<JavadocElement> traverse() {
        var nodes = new ArrayList<JavadocElement>();
        visit(nodes::add);
        return nodes;
    }

    /**
     * Parse an element.
     *
     * @param input input
     * @return element, never {@code null}
     */
    static JavadocElement parse(String input) {
        var reader = new JavadocReader(input);
        return reader.readElement();
    }

    /**
     * HTML tree node.
     *
     * @param parent     parent, must be non {@code null}
     * @param name       name, must be non {@code null}
     * @param attributes attributes, must be non {@code null}
     * @param children   children, must be non {@code null}
     */
    record Node(JavadocElement parent, String name, Map<String, String> attributes, List<JavadocElement> children)
            implements JavadocElement {

        /**
         * HTML tree node.
         *
         * @param parent     parent, must be non {@code null}
         * @param name       name, must be non {@code null}
         * @param attributes attributes, must be non {@code null}
         * @param children   children, must be non {@code null}
         */
        public Node(JavadocElement parent, String name, Map<String, String> attributes, List<JavadocElement> children) {
            this.parent = Objects.requireNonNull(parent, "parent null!");
            this.name = Objects.requireNonNull(name, "name is null!");
            this.attributes = Objects.requireNonNull(attributes, "attributes is null!");
            this.children = Objects.requireNonNull(children, "children is null!");
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node node)) {
                return false;
            }
            return Objects.equals(name, node.name)
                   && Objects.equals(attributes, node.attributes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, attributes);
        }
    }

    /**
     * Root node.
     *
     * @param children children, must be non {@code null}
     */
    record RootNode(List<JavadocElement> children) implements JavadocElement {

        /**
         * Root node.
         *
         * @param children children, never {@code null}
         */
        public RootNode(List<JavadocElement> children) {
            this.children = Objects.requireNonNull(children, "children is null!");
        }

        @Override
        public JavadocElement parent() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Text node.
     *
     * @param parent parent
     * @param value  value
     */
    record TextNode(JavadocElement parent, String value) implements JavadocElement {

        /**
         * Text node.
         *
         * @param parent parent
         * @param value  value, must be non {@code null}
         */
        public TextNode(JavadocElement parent, String value) {
            this.parent = Objects.requireNonNull(parent, "parent null!");
            this.value = Objects.requireNonNull(value, "value null!");
        }

        @Override
        public List<JavadocElement> children() {
            return List.of();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TextNode text)) {
                return false;
            }
            return Objects.equals(value, text.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }
}
