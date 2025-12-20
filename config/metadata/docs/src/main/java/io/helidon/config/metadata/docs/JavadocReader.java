package io.helidon.config.metadata.docs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import io.helidon.config.metadata.docs.JavadocElement.Node;
import io.helidon.config.metadata.docs.JavadocElement.RootNode;
import io.helidon.config.metadata.docs.JavadocElement.TextNode;
import io.helidon.config.metadata.docs.JavadocParser.Event;

/**
 * Javadoc reader.
 */
class JavadocReader {

    private final JavadocParser parser;

    JavadocReader(String input) {
        this.parser = new JavadocParser(input);
    }

    /**
     * Read an element.
     *
     * @return element, never {@code null}
     */
    JavadocElement readElement() {
        var root = new RootNode(new ArrayList<>());
        JavadocElement node = root;
        while (parser.hasNext()) {
            var event = parser.next();
            switch (event) {
                case Event.EltStart(String name) -> {
                    node = new Node(node, name, readAttributes(), new ArrayList<>());
                    node.parent().children().add(node);
                }
                case Event.EltClose() -> {
                    node = node.parent();
                    if (node == null) {
                        throw new IllegalStateException("Unexpected close element: location=" + parser.cursor());
                    }
                }
                case Event.Cdata(String str) -> node.children().add(new TextNode(node, str));
                case Event.Text(String str) -> {
                    if (!str.isBlank()) {
                        node.children().add(new TextNode(node, str));
                    }
                }
                default -> {
                    // ignore
                }
            }
        }
        return root;
    }

    /**
     * Read attributes.
     *
     * @return attributes, never {@code null}
     */
    Map<String, String> readAttributes() {
        Map<String, String> attrs = null;
        String key = null;
        while (parser.hasNext()) {
            var event = parser.peek();
            switch (event) {
                case Event.AttrName(String name) -> {
                    parser.skip();
                    if (attrs == null) {
                        attrs = new LinkedHashMap<>();
                    }
                    attrs.put(name, "");
                    key = name;
                }
                case Event.AttrValue(String value) -> {
                    parser.skip();
                    if (attrs == null) {
                        attrs = new LinkedHashMap<>();
                    }
                    attrs.put(key, value);
                }
                default -> {
                    return attrs == null ? Map.of() : attrs;
                }
            }
        }
        throw new IllegalStateException("Unexpected EOF");
    }
}
