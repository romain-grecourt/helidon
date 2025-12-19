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
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

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
        var result = compile(new ProcessorImpl(comments), new JavaSourceFromString("JavadocEscapes", """
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

    static class ProcessorImpl extends AbstractProcessor {

        private final List<String> comments;

        ProcessorImpl(List<String> comments) {
            this.comments = comments;
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            var utils = processingEnv.getElementUtils();
            for (var rootElement : roundEnv.getRootElements()) {
                comments.add(utils.getDocComment(rootElement));
            }
            return true;
        }

        @Override
        public Set<String> getSupportedAnnotationTypes() {
            return Set.of("*");
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.RELEASE_21;
        }
    }
}
