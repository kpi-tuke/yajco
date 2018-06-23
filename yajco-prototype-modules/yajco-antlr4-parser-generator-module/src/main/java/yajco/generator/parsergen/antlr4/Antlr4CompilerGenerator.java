package yajco.generator.parsergen.antlr4;

import org.antlr.v4.Tool;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.generator.parsergen.antlr4.model.Grammar;
import yajco.model.Language;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.net.URI;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class Antlr4CompilerGenerator implements CompilerGenerator {
    static final private String ANTLR4_PARSER_CLASS_TEMPLATE = "/yajco/generator/parsergen/antlr4/templates/Parser.javavm";
    private VelocityEngine velocityEngine;

    // TODO: Remove this hack later.
    static private class SystemExitException extends SecurityException {
        private final int status;

        public SystemExitException(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
        generateFiles(language, filer, properties, "UnnamedParser");
    }

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties, String parserFullClassName) {
        if (this.velocityEngine == null) {
            this.velocityEngine = new VelocityEngine();
        }

        try {
            final int lastDotPos = parserFullClassName.lastIndexOf(".");
            final String parserClassName = parserFullClassName.substring(lastDotPos + 1);
            final String parserPackageName = parserFullClassName.substring(0, lastDotPos);
            final String ANTLRParserPackageName = parserPackageName + ".antlr4";
            final String ANTLRParserClassName = parserClassName + "Parser";
            final String ANTLRLexerClassName = parserClassName + "Lexer";

            final String grammarName = parserClassName;
            final String grammarFileName = grammarName + ".g4";

            // Create ANTLR4 grammar specification
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT, ANTLRParserPackageName, grammarFileName);
            try (Writer writer = fileObject.openWriter()) {
                ModelTranslator translator = new ModelTranslator(language, grammarName, ANTLRParserPackageName);
                Grammar grammar = translator.translate();
                System.out.println("\nGenerated ANTLR4 grammar:");
                System.out.println("--------------------------------------------------------------------------------------------------------");
                System.out.println(grammar.generate());
                System.out.println("--------------------------------------------------------------------------------------------------------");
                writer.write(grammar.generate());
            }

            URI grammarURI = fileObject.toUri();

            final String[] args = new String[] {
                "-o", new File(grammarURI).getParent(), // Output directory.
                "-no-listener", // Don't generate listeners or visitors; we only need the parser.
                "-no-visitor",
                grammarURI.getPath() // Path of the grammar file.
            };

            // These will be later overwritten when we run the ANTLR tool. We have to do this so they
            // are registered for compilation.
            try (Writer writer = filer.createSourceFile(ANTLRParserPackageName + "." + ANTLRParserClassName).openWriter()) {
            }
            try (Writer writer = filer.createSourceFile(ANTLRParserPackageName + "." + ANTLRLexerClassName).openWriter()) {
            }

            // Run the ANTLR tool.
            // FIXME: Is this part of the public API? Maybe there is a better way, less prone to compatibility breakage?
            // FIXME: We use a nasty hack to catch System.exit called from Tool.main.
            SecurityManager oldSecurityManager = System.getSecurityManager();
            System.setSecurityManager(new SecurityManager() {
                @Override
                public void checkPermission(Permission perm)
                {
                }

                @Override
                public void checkExit(int status)
                {
                    throw new SystemExitException(status);
                }
            });
            try {
                Tool.main(args);
            } catch (SystemExitException e) {
                if (e.getStatus() != 0)
                    throw new RuntimeException("ANTLR4 exited with a non-zero code");
            } finally {
                // Restore old security manager
                System.setSecurityManager(oldSecurityManager);
            }

            // Create parser class wrapping the ANTLR-generated one.
            try (Writer writer = filer.createSourceFile(parserFullClassName).openWriter()) {
                writer.write(generateParserWrapper(
                    ANTLRParserPackageName + "." + ANTLRParserClassName,
                    ANTLRParserPackageName + "." + ANTLRLexerClassName,
                    parserPackageName,
                    parserClassName,
                    yajco.model.utilities.Utilities.getFullConceptClassName(language, language.getConcepts().get(0))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("ANTLR4 Compiler Generator reports an error: " + e.getMessage());
        }
    }

    private String generateParserWrapper(String ANTLRParserFullClassName, String ANTLRLexerFullClassName,
                                         String parserPackageName, String parserClassName,
                                         String mainElementClassName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("ANTLRParserFullClassName", ANTLRParserFullClassName);
        context.put("ANTLRLexerFullClassName", ANTLRLexerFullClassName);
        context.put("parserPackageName", parserPackageName);
        context.put("parserClassName", parserClassName);
        context.put("mainElementClassName", mainElementClassName);
        context.put("returnVarName", ModelTranslator.RETURN_VAR_NAME);

        StringWriter writer = new StringWriter();
        this.velocityEngine.evaluate(context, writer, "",
                new InputStreamReader(getClass().getResourceAsStream(ANTLR4_PARSER_CLASS_TEMPLATE)));
        return writer.toString();
    }
}
