/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

package io.helidon.config.metadata.codegen;

import javax.lang.model.element.Element;

import io.helidon.codegen.RoundContext;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.SummaryTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;

final class Javadoc {

    private Javadoc() {
    }

    // for existing usages
    static String parse(RoundContext roundContext, TypeInfo type, TypedElementInfo element) {
        return parse(roundContext, type, element, true);
    }

    /**
     * Parses a Javadoc comment (provided as a string) into text that can be used for display/docs of the configuration option.
     * <p>
     * The following steps are done:
     * <ul>
     *     <li>{@code @param} is stripped from the text</li>
     *     <li>Any {@code @code} section: the code tag is removed, and surrounded with {@code '}</li>
     *     <li>Any {@code @link} section: the link tag is removed</li>
     *     <li>Any {@code @linkplain} section: the linkplain tag is removed</li>
     *     <li>Any {@code @value} section: the value tag is removed, {code #} is replaced with {@code .}</li>
     *     <li>Any {@code @see} section: the see tag is removed, prefixed with {@code See},
     *                  {code #} is replaced with {@code .}</li>
     *     <li>{@code @return} is stripped from the text, and the first letter is capitalized</li>
     * </ul>
     *
     * @param roundContext  round context
     * @param typeInfo      type info
     * @param elementInfo   element info
     * @param includeReturn whether to include the return
     * @return description of the option
     */
    @SuppressWarnings("removal")
    static String parse(RoundContext roundContext, TypeInfo typeInfo, TypedElementInfo elementInfo, boolean includeReturn) {
        StringBuilder sb = new StringBuilder();
        if (roundContext.sharedContext() instanceof io.helidon.codegen.apt.AptContext aptContext) {
            var orig = elementInfo.originatingElement().orElse(null);
            if (orig instanceof Element elt) {
                var aptEnv = aptContext.aptEnv();
                var dc = DocTrees.instance(aptEnv);
                var dct = dc.getDocCommentTree(elt);
                if (dct != null) {
                    var visitor = new JavadocVisitor(roundContext, typeInfo, includeReturn);
                    for (var e : dct.getFullBody()) {
                        e.accept(visitor, sb);
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    private static final class JavadocVisitor extends SimpleDocTreeVisitor<StringBuilder, StringBuilder> {
        private final RoundContext ctx;
        private final TypeInfo typeInfo;
        private final boolean includeReturn;

        JavadocVisitor(RoundContext ctx, TypeInfo typeInfo, boolean includeReturn) {
            this.ctx = ctx;
            this.typeInfo = typeInfo;
            this.includeReturn = includeReturn;
        }

        @Override
        public StringBuilder visitLink(LinkTree node, StringBuilder sb) {
            sb.append("<code>");
            sb.append(node.getReference().getSignature());
            sb.append("</code>");
            return sb;
        }

        @Override
        public StringBuilder visitLiteral(LiteralTree node, StringBuilder sb) {
            if (node.getKind() == DocTree.Kind.CODE) {
                sb.append("<code>");
                sb.append(node.getBody().getBody());
                sb.append("</code>");
            } else {
                sb.append(node.getBody().getBody());
            }
            return sb;
        }

        @Override
        public StringBuilder visitValue(ValueTree node, StringBuilder sb) {
            var ref = node.getReference();
            if (ref != null) {
                var signature = ref.getSignature();
                var index = signature.indexOf("#");
                if (index >= 0) {
                    TypeInfo refType = typeInfo;
                    if (index > 0) {
                        var typeName = TypeName.create(signature.substring(0, index - 1));
                        refType = ctx.typeInfo(typeName).orElse(null);
                    }
                    if (refType != null) {
                        var field = signature.substring(index + 1, signature.length() - 1);
                        for (TypedElementInfo e : refType.elementInfo()) {
                            if (e.kind() == ElementKind.FIELD && field.equals(e.elementName())) {
                                sb.append("<code>");
                                sb.append(e.originatingElementValue());
                                sb.append("</code>");
                                break;
                            }
                        }
                    }
                }
            }
            return sb;
        }

        @Override
        public StringBuilder visitText(TextTree node, StringBuilder sb) {
            sb.append(node.getBody());
            return sb;
        }

        @Override
        public StringBuilder visitEntity(EntityTree node, StringBuilder sb) {
            sb.append("&");
            sb.append(node.getName());
            sb.append(";");
            return sb;
        }

        @Override
        public StringBuilder visitSummary(SummaryTree node, StringBuilder sb) {
            sb.append("<summary>\n");
            for (var e : node.getSummary()) {
                e.accept(this, sb);
            }
            sb.append("</summary>\n");
            return sb;
        }

        @Override
        public StringBuilder visitSee(SeeTree node, StringBuilder sb) {
            sb.append("\nSee:\n");
            sb.append("<ul>\n");
            for (var e : node.getReference()) {
                sb.append("<li>");
                e.accept(this, sb);
                sb.append("</li>\n");
            }
            sb.append("</ul>\n");
            return sb;
        }

        @Override
        public StringBuilder visitReturn(ReturnTree node, StringBuilder sb) {
            if (includeReturn) {
                for (var e : node.getDescription()) {
                    e.accept(this, sb);
                }
            }
            return sb;
        }

        @Override
        public StringBuilder visitStartElement(StartElementTree node, StringBuilder sb) {
            sb.append("<");
            sb.append(node.getName());
            for (var e : node.getAttributes()) {
                e.accept(this, sb);
            }
            sb.append(">");
            return sb;
        }

        @Override
        public StringBuilder visitAttribute(AttributeTree node, StringBuilder sb) {
            switch (node.getValueKind()) {
                case EMPTY -> {
                    sb.append(" ");
                    sb.append(node.getName());
                }
                case SINGLE -> {
                    for (var v : node.getValue()) {
                        sb.append(" ");
                        sb.append(node.getName());
                        sb.append("='");
                        v.accept(this, sb);
                        sb.append("'");
                    }
                }
                case DOUBLE -> {
                    for (var v : node.getValue()) {
                        sb.append(" ");
                        sb.append(node.getName());
                        sb.append("=\"");
                        v.accept(this, sb);
                        sb.append("\"");
                    }
                }
                case UNQUOTED -> {
                    for (var v : node.getValue()) {
                        sb.append(" ");
                        sb.append(node.getName());
                        sb.append("=");
                        v.accept(this, sb);
                    }
                }
            }
            return sb;
        }

        @Override
        public StringBuilder visitEndElement(EndElementTree node, StringBuilder sb) {
            sb.append("</");
            sb.append(node.getName());
            sb.append(">");
            return sb;
        }
    }
}
