package yajco.annotation.processor;

import org.junit.rules.TemporaryFolder;
import yajco.model.Language;
import yajco.model.utilities.XMLLanguageFormatHelper;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Java compiler harness for annotation processor tests.
 */
final class AnnotationProcessorTestCompiler {

    private final TemporaryFolder temporaryFolder;

    AnnotationProcessorTestCompiler(TemporaryFolder temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
    }

    Language compileAndReadLanguageModel(String className, String source) throws IOException {
        return compileAndReadLanguageModel(source(className, source));
    }

    Language compileAndReadLanguageModel(SourceSpec... sources) throws IOException {
        CompilationResult result = compile(sources);
        assertTrue(result.diagnosticsToString(), result.success);

        Path modelXml = result.classOutput.toPath().resolve("META-INF/yajco-lang.xml");
        assertTrue("Expected generated language model at " + modelXml, Files.exists(modelXml));
        try (InputStream inputStream = Files.newInputStream(modelXml)) {
            return XMLLanguageFormatHelper.readFromXML(inputStream);
        }
    }

    List<Diagnostic<? extends JavaFileObject>> compileExpectingErrors(SourceSpec... sources) throws IOException {
        CompilationResult result = compile(sources);
        assertFalse("Expected compilation to fail, but it succeeded", result.success);
        return result.errors();
    }

    private CompilationResult compile(SourceSpec... sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("Tests must run on a JDK, not a JRE", compiler);

        File classOutput = temporaryFolder.newFolder("classes");
        File sourceOutput = temporaryFolder.newFolder("generated-sources");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                 compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            List<String> options = Arrays.asList(
                "-classpath", testClasspath(),
                "-d", classOutput.getAbsolutePath(),
                "-s", sourceOutput.getAbsolutePath(),
                "-proc:only");
            JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                sourceFiles(sources));
            task.setProcessors(Collections.singletonList(new AnnotationProcessor()));
            return new CompilationResult(task.call(), classOutput, diagnostics.getDiagnostics());
        }
    }

    private static final class CompilationResult {
        final boolean success;
        final File classOutput;
        final List<Diagnostic<? extends JavaFileObject>> diagnostics;

        CompilationResult(boolean success, File classOutput, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
            this.success = success;
            this.classOutput = classOutput;
            this.diagnostics = diagnostics;
        }

        List<Diagnostic<? extends JavaFileObject>> errors() {
            List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(d);
                }
            }
            return errors;
        }

        String diagnosticsToString() {
            StringBuilder result = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
                result.append(diagnostic.getKind())
                    .append(": ")
                    .append(diagnostic.getMessage(Locale.ROOT))
                    .append(System.lineSeparator());
            }
            return result.toString();
        }
    }

    static SourceSpec source(String className, String source) {
        return new SourceSpec(className, source);
    }

    private List<JavaFileObject> sourceFiles(SourceSpec[] sources) {
        List<JavaFileObject> sourceFiles = new ArrayList<>();
        for (SourceSpec source : sources) {
            sourceFiles.add(new SourceFile(source.className, source.source));
        }
        return sourceFiles;
    }

    private String testClasspath() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String classpath = System.getProperty("java.class.path", "");
        if (classpath == null || classpath.isEmpty()) {
            return classpathFromClassLoader(classLoader);
        }
        return classpath;
    }

    private String classpathFromClassLoader(ClassLoader classLoader) {
        if (!(classLoader instanceof java.net.URLClassLoader)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (java.net.URL url : ((java.net.URLClassLoader) classLoader).getURLs()) {
            if (result.length() > 0) {
                result.append(File.pathSeparator);
            }
            result.append(new File(url.getFile()).getAbsolutePath());
        }
        return result.toString();
    }

    static final class SourceSpec {
        private final String className;
        private final String source;

        private SourceSpec(String className, String source) {
            this.className = className;
            this.source = source;
        }
    }

    private static final class SourceFile extends SimpleJavaFileObject {
        private final String source;

        private SourceFile(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

        @Override
        public OutputStream openOutputStream() {
            return new ByteArrayOutputStream();
        }
    }
}
