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

import java.util.ArrayList;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import io.helidon.codegen.RoundContext;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypedElementInfo;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.SummaryTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;
import com.sun.source.util.Trees;

/**
 * Utility to parse and render the Javadoc into HTML that can be used for the description of the configuration options.
 * <p>
 * The following block tags are rendered, all other block tags are ignored:
 * <ul>
 *     <li>{@code see}: occurrences are rendered as an HTML list (<code>&lt;ul&gt;</code>)</li>
 *     <li>{@code return}: rendered if {@code includeReturn} is {@code true}, with the first letter capitalized</li>
 * </ul>
 * <p>
 * The following inline tags are rendered, all other inline tags are ignored:
 * <ul>
 *     <li>{@code summary}: rendered as <code>&lt;summary&gt;</code></li>
 *     <li>{@code value}: rendered as <code>&lt;code&gt;</code></li>
 *     <li>{@code code}: rendered as <code>&lt;code&gt;</code></li>
 *     <li>{@code literal}: rendered as <code>&lt;code&gt;</code></li>
 *     <li>{@code link}: rendered as <code>&lt;code&gt;</code></li>
 *     <li>{@code linkplain}: rendered as <code>&lt;code&gt;</code></li>
 * </ul>
 * <p>
 * Flow HTML elements are re-rendered from the AST.
 */
final class JavadocProcessor {

    private JavadocProcessor() {
    }

    /**
     * Parse and render the given element Javadoc into HTML.
     *
     * @param roundContext  round context
     * @param typeInfo      type info
     * @param elementInfo   element info
     * @param includeReturn whether to include the return
     * @return description of the option
     * @throws IllegalStateException if {@link DocTrees} cannot be initialized
     */
    @SuppressWarnings("removal")
    static String process(RoundContext roundContext, TypeInfo typeInfo, TypedElementInfo elementInfo, boolean includeReturn) {
        if (roundContext.sharedContext() instanceof io.helidon.codegen.apt.AptContext aCtx) {
            var origType = typeInfo.originatingElement().orElse(null);
            var origElement = elementInfo.originatingElement().orElse(null);
            if (origType instanceof TypeElement type && origElement instanceof Element elt) {
                return process(aCtx.aptEnv(), type, elt, includeReturn);
            }
        }
        throw new IllegalStateException("Unable to initialize " + DocTrees.class.getName());
    }

    /**
     * Parse and render the given element Javadoc into HTML.
     *
     * @param env           processing environment
     * @param type          type
     * @param element       element
     * @param includeReturn whether to include the return
     * @return description of the option
     */
    static String process(ProcessingEnvironment env, TypeElement type, Element element, boolean includeReturn) {
        StringBuilder sb = new StringBuilder();
        var dc = DocTrees.instance(env);
        var dct = dc.getDocCommentTree(element);
        if (dct != null) {
            var visitor = new JavadocVisitor(env, type);
            for (var e : dct.getFullBody()) {
                e.accept(visitor, sb);
            }
            var seeBlocks = new ArrayList<StringBuilder>();
            for (var e : dct.getBlockTags()) {
                if (e instanceof ReturnTree && includeReturn) {
                    var buf = e.accept(visitor, new StringBuilder());
                    for (var i = 0; i < buf.length(); i++) {
                        var c = buf.charAt(i);
                        if (Character.isLetter(c)) {
                            buf.setCharAt(i, Character.toUpperCase(c));
                            break;
                        }
                    }
                    if (!buf.isEmpty()) {
                        sb.append("\n");
                        sb.append(buf);
                        sb.append("\n");
                    }
                } else if (e instanceof SeeTree) {
                    var buf = e.accept(visitor, new StringBuilder());
                    seeBlocks.add(buf);
                }
            }
            if (!seeBlocks.isEmpty()) {
                sb.append("\n");
                sb.append("See:\n");
                sb.append("<ul>\n");
                for (var e : seeBlocks) {
                    sb.append("<li>");
                    sb.append(e);
                    sb.append("</li>\n");
                }
                sb.append("</ul>\n");
            }
        }
        return sb.toString();
    }

    private static final class JavadocVisitor extends SimpleDocTreeVisitor<StringBuilder, StringBuilder> {
        private final TypeElement type;
        private final RefVisitor refVisitor;

        JavadocVisitor(ProcessingEnvironment env, TypeElement type) {
            this.type = type;
            this.refVisitor = new RefVisitor(env);
        }

        @Override
        public StringBuilder visitLink(LinkTree node, StringBuilder sb) {
            sb.append("<code>");
            var ref = node.getReference();
            var resolved = ref.accept(refVisitor, type);
            sb.append(resolved != null ? resolved : ref.getSignature());
            sb.append("</code>");
            return sb;
        }

        @Override
        public StringBuilder visitLiteral(LiteralTree node, StringBuilder sb) {
            if (node.getKind() == DocTree.Kind.CODE) {
                sb.append("<code>");
                sb.append(encode(node.getBody().getBody()));
                sb.append("</code>");
            } else {
                sb.append(encode(node.getBody().getBody()));
            }
            return sb;
        }

        @Override
        public StringBuilder visitValue(ValueTree node, StringBuilder sb) {
            String value = null;
            var ref = node.getReference();
            var resolved = ref.accept(refVisitor, type);
            if (resolved instanceof VariableElement v) {
                var constantValue = v.getConstantValue();
                if (constantValue != null) {
                    value = String.valueOf(constantValue);
                }
            }
            sb.append("<code>");
            sb.append(value != null ? encode(value) : ref.getSignature());
            sb.append("</code>");
            return sb;
        }

        @Override
        public StringBuilder visitText(TextTree node, StringBuilder sb) {
            sb.append(encode(node.getBody()));
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
            sb.append("<summary>");
            for (var e : node.getSummary()) {
                e.accept(this, sb);
            }
            sb.append("</summary>\n");
            return sb;
        }

        @Override
        public StringBuilder visitReference(ReferenceTree node, StringBuilder sb) {
            var resolved = node.accept(refVisitor, type);
            sb.append("<code>");
            sb.append(resolved != null ? resolved : node.getSignature());
            sb.append("</code> ");
            return sb;
        }

        @Override
        public StringBuilder visitSee(SeeTree node, StringBuilder sb) {
            for (var e : node.getReference()) {
                e.accept(this, sb);
            }
            return sb;
        }

        @Override
        public StringBuilder visitReturn(ReturnTree node, StringBuilder sb) {
            for (var e : node.getDescription()) {
                e.accept(this, sb);
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
                default -> {
                    // do nothing
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

        static String encode(String str) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '\'':
                        sb.append("&apos;");
                        break;
                    case '"':
                        sb.append("&quot;");
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    private static final class RefVisitor extends SimpleDocTreeVisitor<Element, TypeElement> {

        private final Elements elements;
        private final Trees trees;

        RefVisitor(ProcessingEnvironment env) {
            elements = env.getElementUtils();
            trees = Trees.instance(env);
        }

        @Override
        public Element visitReference(ReferenceTree node, TypeElement type) {
            Element element = null;
            var signature = node.getSignature();
            var index = signature.indexOf("#");
            if (index > 0) {
                // explicit type
                var typeName = signature.substring(0, index);
                var refType = resolveType(type, typeName);
                if (refType != null) {
                    // NOTE: methods are not supported yet
                    var memberName = signature.substring(index + 1);
                    element = resolveField(refType, memberName);
                }
            } else if (index == 0) {
                // no type, either current type, or static import
                var memberName = signature.substring(1);
                element = resolveField(type, memberName);
                if (element == null) {
                    element = resolveImportedField(type, memberName);
                }
            } else {
                element = resolveType(type, signature);
            }
            return element;
        }

        private CompilationUnitTree compilationUnit(TypeElement type) {
            var path = trees.getPath(type);
            if (path == null) {
                throw new IllegalStateException("Unable to get compilation unit for: " + type);
            } else {
                return path.getCompilationUnit();
            }
        }

        private TypeElement resolveInnerType(TypeElement enclosingType, String typeName) {
            for (var e : enclosingType.getEnclosedElements()) {
                if (e instanceof TypeElement innerType) {
                    var innerName = innerType.getSimpleName().toString();
                    if (innerName.equals(typeName)) {
                        return innerType;
                    }
                    var index = typeName.indexOf('.');
                    if (typeName.length() > index + 1 && innerName.length() == index) {
                        var enclosingName = typeName.substring(0, index);
                        if (enclosingName.equals(innerName)) {
                            var enclosedName = typeName.substring(index + 1);
                            return resolveInnerType(innerType, enclosedName);
                        }
                    }
                }
            }
            return null;
        }

        private TypeElement resolveImport(String importName, String typeName) {
            if (importName.endsWith(typeName)) {
                return elements.getTypeElement(importName);
            }
            var index = typeName.lastIndexOf('.');
            if (index > 0) {
                var enclosingName = typeName.substring(0, index);
                if (importName.endsWith(enclosingName)) {
                    var enclosingType = resolveImport(importName, enclosingName);
                    if (enclosingType != null) {
                        var enclosedName = typeName.substring(index + 1);
                        return resolveInnerType(enclosingType, enclosedName);
                    }
                }
            }
            return null;
        }

        private TypeElement resolveType(TypeElement currentType, String typeName) {
            // search by qualified key in current module first
            var module = elements.getModuleOf(currentType);
            if (module != null) {
                var resolved = elements.getTypeElement(module, typeName);
                if (resolved != null) {
                    return resolved;
                }
            }

            // search inner classes
            var resolved = resolveInnerType(currentType, typeName);
            if (resolved != null) {
                return resolved;
            }

            // check in this package
            var pkg = elements.getPackageOf(currentType);
            resolved = elements.getTypeElement(pkg.getQualifiedName() + "." + typeName);
            if (resolved != null) {
                return resolved;
            }

            // search imports
            var compilationUnit = compilationUnit(currentType);
            for (var i : compilationUnit.getImports()) {
                var importName = i.getQualifiedIdentifier().toString();
                if (importName.charAt(importName.length() - 1) == '*') {
                    var refName = importName.substring(0, importName.length() - 2);
                    // on-demand import
                    if (!i.isStatic()) {
                        pkg = elements.getPackageElement(refName);
                        if (pkg != null) {
                            for (var e : pkg.getEnclosedElements()) {
                                if (e instanceof TypeElement te) {
                                    resolved = resolveImport(te.toString(), typeName);
                                    if (resolved != null) {
                                        return resolved;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // qualified import
                    resolved = resolveImport(importName, typeName);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }

            // search by qualified key in all modules
            resolved = elements.getTypeElement(typeName);
            if (resolved != null) {
                return resolved;
            }

            // finally, try java.lang
            return elements.getTypeElement("java.lang." + typeName);
        }

        VariableElement resolveField(TypeElement refType, String fieldName) {
            for (var e : refType.getEnclosedElements()) {
                if (e instanceof VariableElement v && v.getSimpleName().contentEquals(fieldName)) {
                    return v;
                }
            }
            return null;
        }

        VariableElement resolveImportedField(TypeElement currentType, String fieldName) {
            var compilationUnit = compilationUnit(currentType);
            for (var e : compilationUnit.getImports()) {
                if (e.isStatic()) {
                    var importName = e.getQualifiedIdentifier().toString();
                    if (importName.charAt(importName.length() - 1) == '*') {
                        // wildcard static import
                        var staticRefName = importName.substring(0, importName.length() - 2);
                        var staticRefType = elements.getTypeElement(staticRefName);
                        if (staticRefType != null) {
                            var field = resolveField(staticRefType, fieldName);
                            if (field != null) {
                                return field;
                            }
                        }
                    } else {
                        // qualified static import
                        var index = importName.lastIndexOf('.');
                        if (index > 0) {
                            var importedName = importName.substring(index + 1);
                            if (importedName.equals(fieldName)) {
                                var staticRefName = importName.substring(0, index);
                                var staticRefType = elements.getTypeElement(staticRefName);
                                if (staticRefType != null) {
                                    return resolveField(staticRefType, fieldName);
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
