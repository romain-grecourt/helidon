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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.helidon.config.metadata.docs.JavadocParser.Event;

/**
 * Javadoc element.
 */
class JavadocElement {

    // TODO:
    //  - model as sealed interface (HTMLNode, BlockNode, InlineNode, TextNode)

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

    private final List<JavadocElement> children = new ArrayList<>();
    private final Map<String, String> attributes;
    private final String name;
    private final JavadocElement parent;
    private String value;

    /**
     * Create a new instance.
     *
     * @param name name
     */
    JavadocElement(String name) {
        this(null, name, Map.of(), "");
    }

    /**
     * Create a new instance.
     *
     * @param parent     parent, may be {@code null}
     * @param name       name
     * @param attributes attributes
     */
    JavadocElement(JavadocElement parent, String name, Map<String, String> attributes, String value) {
        this.parent = parent;
        this.name = Objects.requireNonNull(name, "name is null");
        this.attributes = Objects.requireNonNull(attributes, "attributes is null");
        this.value = Objects.requireNonNull(value, "value is null");
    }

    /**
     * Get the parent element.
     *
     * @return parent, or {@code null}
     */
    JavadocElement parent() {
        return parent;
    }

    /**
     * Get the element name.
     *
     * @return name, never {@code null}
     */
    String name() {
        return name;
    }

    /**
     * Get the element value.
     *
     * @return value, never {@code null}
     */
    String value() {
        return value;
    }

    /**
     * Get the children.
     *
     * @return chidlren, never {@code null}
     */
    List<JavadocElement> children() {
        return children;
    }

    /**
     * Get the attributes.
     *
     * @return attributes, never {@code null}
     */
    Map<String, String> attributes() {
        return attributes;
    }

    /**
     * Traverse this node (depth-first).
     *
     * @return list of nodes
     */
    List<JavadocElement> traverse() {
        var nodes = new ArrayList<JavadocElement>();
        visit(nodes::add);
        return nodes;
    }

    /**
     * Visit this element.
     *
     * @param visitor visitor
     */
    void visit(Visitor visitor) {
        Deque<JavadocElement> stack = new ArrayDeque<>();
        stack.push(this);
        JavadocElement parent = this.parent();
        while (!stack.isEmpty()) {
            JavadocElement elt = stack.peek();
            if (elt == parent) {
                visitor.postVisitElement(elt);
                parent = elt.parent();
                stack.pop();
            } else {
                visitor.visitElement(elt);
                List<JavadocElement> children = elt.children();
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JavadocElement that)) {
            return false;
        }
        return Objects.equals(children, that.children)
               && Objects.equals(attributes, that.attributes)
               && Objects.equals(name, that.name)
               && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(children, attributes, name, parent, value);
    }

    @Override
    public String toString() {
        return "JavadocElement{" +
               ", name='" + name + '\'' +
               ", attributes=" + attributes +
               ", value='" + value + '\'' +
               '}';
    }

    /**
     * Parse a document.
     *
     * @param input content to parse
     * @return element, never null
     */
    static JavadocElement parse(String input) {
        var parser = new JavadocParser(input);
        var root = new JavadocElement(null, "", Map.of(), "");
        var node = root;
        var sb = new StringBuilder();
        while (parser.hasNext()) {
            var event = parser.next();
            switch (event) {
                case Event.EltStart(String name) -> {
                    sb.setLength(0);
                    node = new JavadocElement(node, name, readAttributes(parser), "");
                    node.parent.children.add(node);
                }
                case Event.EltClose() -> {
                    node.value = sb.toString();
                    sb.setLength(0);
                    node = node.parent;
                    if (node == null) {
                        throw new IllegalStateException("Unexpected close element: location=" + parser.cursor());
                    }
                }
                case Event.Cdata(String str) -> sb.append(str);
                case Event.Text(String str) -> {
                    if (!str.isBlank()) {
                        sb.append(str);
                    }
                }
                default -> {
                    // ignore
                }
            }
        }
        return root;
    }

    private static Map<String, String> readAttributes(JavadocParser parser) {
        Map<String, String> attributes = null;
        String key = null;
        while (parser.hasNext()) {
            var event = parser.peek();
            switch (event) {
                case Event.AttrName(String name) -> {
                    parser.skip();
                    if (key != null) {
                        if (attributes == null) {
                            attributes = new LinkedHashMap<>();
                        }
                        attributes.put(key, "");
                    }
                    key = name;
                }
                case Event.AttrValue(String value) -> {
                    parser.skip();
                    if (attributes == null) {
                        attributes = new LinkedHashMap<>();
                    }
                    attributes.put(key, value);
                }
                default -> {
                    return attributes == null ? Map.of() : attributes;
                }
            }
        }
        throw new IllegalStateException("Unexpected EOF");
    }
}

