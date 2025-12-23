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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.doctree.DocTree;
import com.sun.source.util.DocTrees;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Demonstrates the behavior of {@link javax.lang.model.util.Elements#getDocComment(javax.lang.model.element.Element)}.
 */
class JavadocCommentTest {

    @Test
    void testEscapes() {
        var comments = new ArrayList<String>();
        var result = compile(new BaseProcessor() {
            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                var utils = processingEnv.getElementUtils();
                for (var rootElement : roundEnv.getRootElements()) {
                    comments.add(utils.getDocComment(rootElement));
                }
                return true;
            }
        }, new JavaSourceFromString("JavadocEscapes", """
                /**
                 *@@
                 *@/
                 **@*
                 */
                 class JavadocEscapes {
                 }
                """));
        assertThat(result, is(true));
        assertThat(comments, is(List.of("@@\n@/\n@*\n")));
    }

    @Test
    void escapeCloseCurly() {
        var docTrees = new LinkedHashMap<Element, List<? extends DocTree>>();
        var result = compile(new BaseProcessor() {
            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                var dc = DocTrees.instance(processingEnv);
                for (var rootElement : roundEnv.getRootElements()) {
                    var dct = dc.getDocCommentTree(rootElement);
                    if (dct != null) {
                        List<? extends DocTree> fullBody = dct.getFullBody();
                        docTrees.put(rootElement, dct.getFullBody());
                    }
                    for (var e : rootElement.getEnclosedElements()) {
                        dct = dc.getDocCommentTree(e);
                        if (dct != null) {
                            docTrees.put(e, dct.getFullBody());
                        }
                    }
                }
                return true;
            }
        }, new JavaSourceFromString("Http2Config", """
                /**
                 * HTTP/2 server configuration.
                 */
                 public class Http2Config {
                    private static final int CST = 69;
                     /**
                      * Value of CST is {@value #CST}.
                      * Outbound flow control blocking timeout configured as {@link java.time.Duration}
                      * or text in ISO-8601 format.
                      * Blocking timeout defines an interval to wait for the outbound window size changes(incoming window updates).
                      * Default value is {@code PT15S}.
                      *
                      * <table>
                      *     <caption><b>ISO_8601 format examples:</b></caption>
                      *     <tr><th>PT0.1S</th><th>100 milliseconds</th></tr>
                      *     <tr><th>PT0.5S</th><th>500 milliseconds</th></tr>
                      *     <tr><th>PT2S</th><th>2 seconds</th></tr>
                      * </table>
                      *
                      * @return duration
                      * @see <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">ISO_8601 Durations</a>
                      */
                     String flowControlTimeout() {
                        return "PT15S";
                     }
                 }
                """));
        assertThat(result, is(true));
        assertThat(docTrees.size(), is(2));
        for (var entry : docTrees.entrySet()) {
            var element = entry.getKey();
            var docTree = entry.getValue();
            switch (element.getKind()) {
                case CLASS -> {
                    var name = element.getSimpleName().toString();
                    assertThat(name, is("Http2Config"));
                }
                case METHOD -> {
                    var name = element.getSimpleName().toString();
                    assertThat(name, is("flowControlTimeout"));
                }
                default -> throw new AssertionError("Unexpected element kind: " + element.getKind());
            }
        }
    }

    static List<String> COMPILER_OPTS = List.of(
            "--release", "21",
            "-proc:only");

    static boolean compile(Processor processor, JavaFileObject... sources) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<>();
        var manager = compiler.getStandardFileManager(diagnostics, null, null);
        try {
            manager.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of());
            manager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(Files.createTempDirectory("javac")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var task = compiler.getTask(null, manager, diagnostics, COMPILER_OPTS, null, List.of(sources));
        task.setProcessors(List.of(processor));
        var success = task.call();
        for (var diagnostic : diagnostics.getDiagnostics()) {
            System.err.println(diagnostic);
        }
        return success;
    }

    static class JavaSourceFromString extends SimpleJavaFileObject {

        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    static abstract class BaseProcessor extends AbstractProcessor {

        @Override
        public Set<String> getSupportedAnnotationTypes() {
            return Set.of("*");
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }
    }
}
