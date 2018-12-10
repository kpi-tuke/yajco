package yajco.generator.parsergen.antlr4;

import org.antlr.v4.Tool;
import org.antlr.v4.runtime.Token;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.generator.parsergen.antlr4.translator.ModelTranslator;
import yajco.generator.parsergen.antlr4.model.Grammar;
import yajco.generator.util.Utilities;
import yajco.model.Language;
import yajco.model.SkipDef;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.net.URI;
import java.security.Permission;
import java.util.*;

public class Antlr4CompilerGenerator implements CompilerGenerator {
    static final private String ANTLR4_PARSER_CLASS_TEMPLATE = "/yajco/generator/parsergen/antlr4/templates/Parser.javavm";
    static final private String ANTLR4_LEXER_CLASS_TEMPLATE = "/yajco/generator/parsergen/antlr4/templates/Lexer.javavm";
    static final private String ANTLR4_PARSE_EXCEPTION_CLASS_TEMPLATE = "/yajco/generator/parsergen/antlr4/templates/ParseException.javavm";
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

            ModelTranslator translator = new ModelTranslator(language, grammarName, ANTLRParserPackageName);

            // Create ANTLR4 grammar specification
            FileObject fileObject = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT, ANTLRParserPackageName, grammarFileName);
            try (Writer writer = fileObject.openWriter()) {
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
            FileObject lexerFileObject = filer.createSourceFile(ANTLRParserPackageName + "." + ANTLRLexerClassName);

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

            // Create parse exception
            try (Writer writer = filer.createSourceFile(parserPackageName + ".ParseException").openWriter()) {
                writer.write(generateParseException(parserPackageName));
            }

            // Create lexer, overwriting the ANTLR generated one (which is empty anyway)
            try (Writer writer = lexerFileObject.openWriter()) {
                writer.write(generateLexer(ANTLRParserPackageName, ANTLRLexerClassName,
                        translator.getOrderedTokens(), language.getSkips()));
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

    private String generateParseException(String parserPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserPackageName", parserPackageName);

        StringWriter writer = new StringWriter();
        this.velocityEngine.evaluate(context, writer, "",
                new InputStreamReader(getClass().getResourceAsStream(ANTLR4_PARSE_EXCEPTION_CLASS_TEMPLATE)));
        return writer.toString();
    }

    public String generateLexer(String lexerPackageName, String lexerClassName, Map<String, String> tokens, List<SkipDef> skips) {
        VelocityContext context = new VelocityContext();
        StringWriter writer = new StringWriter();
        context.put("lexerPackageName", lexerPackageName);
        context.put("lexerClassName", lexerClassName);
        context.put("tokens", tokens);
        context.put("skips", skips);
        context.put("firstUserTokenType", Token.MIN_USER_TOKEN_TYPE);
        context.put("Utilities", Utilities.class);
        this.velocityEngine.evaluate(context, writer, "",
                new InputStreamReader(getClass().getResourceAsStream(ANTLR4_LEXER_CLASS_TEMPLATE)));
        return writer.toString();
    }
}
