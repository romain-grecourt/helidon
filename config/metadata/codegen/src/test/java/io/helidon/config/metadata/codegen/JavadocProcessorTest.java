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
package io.helidon.config.metadata.codegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
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
 * Tests {@link JavadocProcessor}.
 */
class JavadocProcessorTest {

    @Test
    void testTypeFirstSentence() {
        var processor = new ProcessorImpl();
        var result = compile(processor, new JavaSourceFromString("TestTypeFirstSentence", """
                /**
                 * The first sentence.
                 * <p>
                 * A paragraph.
                 * </p>
                 */
                class TestTypeFirstSentence {
                }
                """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("The first sentence.\n <p>\n A paragraph.\n </p>"));
    }

    @Test
    void testMethodFirstSentence() {
        var processor = new ProcessorImpl();
        var result = compile(processor, new JavaSourceFromString("TestMethodFirstSentence", """
                class TestMethodFirstSentence {
                
                    /**
                     * The first sentence.
                     * <p>
                     * A paragraph.
                     * </p>
                     */
                    void method() {
                    }
                 }
                """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("The first sentence.\n <p>\n A paragraph.\n </p>"));
    }

    @Test
    void testLink() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Stuff", """
                        package com.acme;
                        public class Stuff {
                        }
                        """),
                new JavaSourceFromString("TestLink", """
                        /**
                         * A link to {@link com.acme.Stuff}.
                         */
                        class TestLink {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("A link to <code>com.acme.Stuff</code>."));
    }

    @Test
    void testImportedLink() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Stuff", """
                        package com.acme;
                        public class Stuff {
                        }
                        """),
                new JavaSourceFromString("TestImportedLink", """
                        import com.acme.Stuff;
                        /**
                         * A link to {@link Stuff}.
                         */
                        class TestImportedLink {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("A link to <code>com.acme.Stuff</code>."));
    }

    @Test
    void testLinkWithLabel() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                        }
                        """),
                new JavaSourceFromString("TestLinkWithLabel", """
                        /**
                         * A link to {@link com.acme.Holder holder class}.
                         */
                        class TestLinkWithLabel {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("A link to <code>com.acme.Holder</code>."));
    }

    @Test
    void testLinkPlain() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                        }
                        """),
                new JavaSourceFromString("TestLinkPlain", """
                        /**
                         * A link to {@linkplain com.acme.Holder}.
                         */
                        class TestLinkPlain {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("A link to <code>com.acme.Holder</code>."));
    }

    @Test
    void testLinkPlainWithLabel() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                        }
                        """),
                new JavaSourceFromString("TestLinkPlainWithLabel", """
                        /**
                         * A link to {@linkplain com.acme.Holder holder class}.
                         */
                        class TestLinkPlainWithLabel {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("A link to <code>com.acme.Holder</code>."));
    }

    @Test
    void testLiteral() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("TestLiteral", """
                        /**
                         * A literal: {@literal <>'"&}.
                         */
                        class TestLiteral {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("A literal: &lt;&gt;&apos;&quot;&amp;."));
    }

    @Test
    void testInlineCode() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("TestInlineCode", """
                        /**
                         * Inline code: {@code <>'"&}.
                         */
                        class TestInlineCode {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Inline code: <code>&lt;&gt;&apos;&quot;&amp;</code>."));
    }

    @Test
    void testSummary() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("TestSummary", """
                        /**
                         * A class with summary.
                         * {@summary The summary}
                         */
                        class TestSummary {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("""
                A class with summary.
                 <summary>The summary</summary>
                """));
    }

    @Test
    void testSee1() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Stuff", """
                        package com.acme;
                        class Stuff {
                            void doStuff() {
                            }
                        }
                        """),
                new JavaSourceFromString("TestSee1", """
                        /**
                         * First sentence.
                         * @see com.acme.Stuff#doStuff() to <b>do stuff</b>
                         */
                        class TestSee1 {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("""
                First sentence.
                See:
                <ul>
                <li><code>com.acme.Stuff#doStuff()</code> to <b>do stuff</b></li>
                </ul>
                """));
    }

    @Test
    void testSee2() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Stuff", """
                        package com.acme;
                        class Stuff {
                            void doStuff() {
                            }
                            void doMoreStuff() {
                            }
                        }
                        """),
                new JavaSourceFromString("TestSee2", """
                        /**
                         * First sentence.
                         * @see com.acme.Stuff#doStuff() to <b>do stuff</b>
                         * @see com.acme.Stuff#doMoreStuff() to <b>do more stuff</b>
                         */
                        class TestSee2 {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("""
                First sentence.
                See:
                <ul>
                <li><code>com.acme.Stuff#doStuff()</code> to <b>do stuff</b></li>
                <li><code>com.acme.Stuff#doMoreStuff()</code> to <b>do more stuff</b></li>
                </ul>
                """));
    }

    @Test
    void testReturn() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("TestSee2", """
                        class TestReturn {
                            /**
                             * Say hello.
                             * @return echoes the input name prefixed with {@code "Hello "}
                             */
                            String sayHello(String name) {
                                return "Hello " + name;
                            }
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("""
                Say hello.
                Echoes the input name prefixed with <code>&quot;Hello &quot;</code>
                """));
    }

    @Test
    void testValueSelfRef() {
        var processor = new ProcessorImpl();
        var result = compile(processor, new JavaSourceFromString("TestValueLocalRef", """
                /**
                 * Default value is {@value #DEFAULT_VALUE}.
                 */
                class TestValueLocalRef {
                    static final int DEFAULT_VALUE = 6;
                }
                """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueQualifiedRef() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                            public static final int DEFAULT_VALUE = 6;
                        }
                        """),
                new JavaSourceFromString("TestValueQualifiedRef", """
                        /**
                         * Default value is {@value com.acme.Holder#DEFAULT_VALUE}.
                         */
                        class TestValueQualifiedRef {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueImportRef1() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                            public static final int DEFAULT_VALUE = 6;
                        }
                        """),
                new JavaSourceFromString("TestValueImportRef1", """
                        import com.acme.Holder;
                        /**
                         * Default value is {@value Holder#DEFAULT_VALUE}.
                         */
                        class TestValueImportRef1 {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueImportRef2() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                            public static class Level1 {
                                public static final int DEFAULT_VALUE = 6;
                            }
                        }
                        """),
                new JavaSourceFromString("TestValueImportRef2", """
                        import com.acme.Holder;
                        /**
                         * Default value is {@value Holder.Level1#DEFAULT_VALUE}.
                         */
                        class TestValueImportRef2 {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueImportRef3() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                            public static class Level1 {
                                public static class Level2 {
                                    public static final int DEFAULT_VALUE = 6;
                                }
                            }
                        }
                        """),
                new JavaSourceFromString("TestValueImportRef3", """
                        import com.acme.Holder.Level1;
                        /**
                         * Default value is {@value Level1.Level2#DEFAULT_VALUE}.
                         */
                        class TestValueImportRef3 {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueWildcardImportRef1() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                            public static final int DEFAULT_VALUE = 6;
                        }
                        """),
                new JavaSourceFromString("TestValueWildcardImportRef", """
                        import com.acme.*;
                        /**
                         * Default value is {@value Holder#DEFAULT_VALUE}.
                         */
                        class TestValueWildcardImportRef {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueStaticImportRef() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                            public static final int DEFAULT_VALUE = 6;
                        }
                        """),
                new JavaSourceFromString("TestValueStaticImportRef", """
                        import static com.acme.Holder.DEFAULT_VALUE;
                        /**
                         * Default value is {@value #DEFAULT_VALUE}.
                         */
                        class TestValueStaticImportRef {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueWildcardStaticImportRef() {
        var processor = new ProcessorImpl();
        var result = compile(processor,
                new JavaSourceFromString("com.acme.Holder", """
                        package com.acme;
                        public class Holder {
                            public static final int DEFAULT_VALUE = 6;
                        }
                        """),
                new JavaSourceFromString("TestValueWildcardStaticImportRef", """
                        import static com.acme.Holder.*;
                        /**
                         * Default value is {@value #DEFAULT_VALUE}.
                         */
                        class TestValueWildcardStaticImportRef {
                        }
                        """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueInnerClassRef1() {
        var processor = new ProcessorImpl();
        var result = compile(processor, new JavaSourceFromString("TestValueInnerClassRef1", """
                /**
                 * Default value is {@value Level1#DEFAULT_VALUE}.
                 */
                class TestValueInnerClassRef1 {
                    static class Level1 {
                        static final int DEFAULT_VALUE = 6;
                    }
                }
                """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueInnerClassRef2() {
        var processor = new ProcessorImpl();
        var result = compile(processor, new JavaSourceFromString("TestValueInnerClassRef2", """
                /**
                 * Default value is {@value Level1.Level2#DEFAULT_VALUE}.
                 */
                class TestValueInnerClassRef2 {
                    static class Level1 {
                        static class Level2 {
                            static final int DEFAULT_VALUE = 6;
                        }
                    }
                }
                """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    @Test
    void testValueInnerClassRef3() {
        var processor = new ProcessorImpl();
        var result = compile(processor, new JavaSourceFromString("TestValueInnerClassRef3", """
                /**
                 * Default value is {@value Level1.Level2.Level3#DEFAULT_VALUE}.
                 */
                class TestValueInnerClassRef3 {
                    static class Level1 {
                        static class Level2 {
                            static class Level3 {
                                static final int DEFAULT_VALUE = 6;
                            }
                        }
                    }
                }
                """));
        assertThat(result, is(true));
        assertThat(processor.javadoc, is("Default value is <code>6</code>."));
    }

    static List<String> COMPILER_OPTS = List.of(
            "--release", "21",
            "-proc:only");

    static boolean compile(Processor processor, JavaFileObject... sources) {
        try {
            var compiler = ToolProvider.getSystemJavaCompiler();
            var diagnostics = new DiagnosticCollector<>();
            var manager = compiler.getStandardFileManager(diagnostics, null, null);
            manager.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of());
            manager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(Files.createTempDirectory("javac")));
            var task = compiler.getTask(null, manager, diagnostics, COMPILER_OPTS, null, List.of(sources));
            task.setProcessors(List.of(processor));
            var success = task.call();
            for (var diagnostic : diagnostics.getDiagnostics()) {
                System.err.println(diagnostic);
            }
            return success;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

        private String javadoc;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            for (var rootElt : roundEnv.getRootElements()) {
                if (rootElt instanceof TypeElement type) {
                    javadoc = JavadocProcessor.process(processingEnv, type, type, true);
                    if (!javadoc.isEmpty()) {
                        break;
                    }
                    for (var e : type.getEnclosedElements()) {
                        javadoc = JavadocProcessor.process(processingEnv, type, e, true);
                        if (!javadoc.isEmpty()) {
                            break;
                        }
                    }
                }
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
