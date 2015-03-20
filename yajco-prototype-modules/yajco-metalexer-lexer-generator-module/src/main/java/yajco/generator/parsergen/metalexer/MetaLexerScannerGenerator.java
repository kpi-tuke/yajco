package yajco.generator.parsergen.metalexer;

import java.io.*;
import java.net.URI;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import metalexer.jflex.ML2JFlex;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.translator.YajcoModelToBNFGrammarTranslator;
import yajco.model.Language;

public class MetaLexerScannerGenerator {

    private static final String METALEXER_DEFAULT_LAYOUT_TEMPLATE = "/yajco/generator/parsergen/metalexer/templates/DefaultLayoutTemplate.vsl";
    private static final String METALEXER_DEFAULT_COMPONENT_TEMPLATE = "/yajco/generator/parsergen/metalexer/templates/DefaultComponentTemplate.vsl";
    private static final String METALEXER_BEAVER_LAYOUT_TEMPLATE = "/yajco/generator/parsergen/metalexer/templates/BeaverLayoutTemplate.vsl";
    private VelocityEngine engine = new VelocityEngine();

    public void generateScanner(ProcessingEnvironment processingEnv, Language language, Grammar grammar, String parserClassName, String parserPackageName) throws IOException {

        //String scannerClassName = parserClassName + "Scanner";
        String metalexerPackageName = parserPackageName + ".metalexer";

        // Default layout
        FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, metalexerPackageName, parserClassName + ".mll");
        Writer writer = fo.openWriter();
        writer.write(generateMetaLexerDefaultLayout(language, grammar, parserClassName, parserPackageName));
        writer.flush();
        writer.close();

        // Beaver helper layout
        fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, metalexerPackageName, "beaver.mll");
        writer = fo.openWriter();
        writer.write(generateMetaLexerBeaverLayout());
        writer.flush();
        writer.close();

        // Default layout
        fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, metalexerPackageName, parserClassName + ".mlc");
        writer = fo.openWriter();
        writer.write(generateMetaLexerDefaultComponent(language, grammar, parserClassName, parserPackageName));
        writer.flush();
        writer.close();

        URI inputFileURI = fo.toUri();
        File inputFile = inputFileURI.isAbsolute() ? new File(inputFileURI) : new File(inputFileURI.toString());
        ML2JFlex.main(new String[]{parserClassName,inputFile.getParent(),inputFile.getParent()});
        
        File jflexFile = new File(inputFile.getParentFile(), parserClassName + ".flex");
        JFlex.Main.main(new String[]{"-d",inputFile.getParentFile().getParent(), jflexFile.getCanonicalPath()});
        //JFlex.Main.generate(jflexFile);
    }

    private String generateMetaLexerDefaultLayout(Language language, Grammar grammar, String parserClassName, String parserPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("language", language);
        context.put("terminals", grammar.getTerminals().values());
        context.put("regexps", grammar.getTerminalPool());
        context.put("parserName", parserClassName);
        context.put("parserPackage", parserPackageName);
        context.put("defaultSymbolName", YajcoModelToBNFGrammarTranslator.DEFAULT_SYMBOL_NAME);
        context.put("Utilities", yajco.generator.util.Utilities.class);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(METALEXER_DEFAULT_LAYOUT_TEMPLATE)));

        return writer.toString();
    }

    private String generateMetaLexerDefaultComponent(Language language, Grammar grammar, String parserClassName, String parserPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("language", language);
        context.put("terminals", grammar.getTerminals().values());
        context.put("regexps", grammar.getTerminalPool());
        context.put("parserName", parserClassName);
        context.put("parserPackage", parserPackageName);
        context.put("defaultSymbolName", YajcoModelToBNFGrammarTranslator.DEFAULT_SYMBOL_NAME);
        context.put("Utilities", yajco.generator.util.Utilities.class);
        context.put("RegexUtilities", yajco.generator.util.RegexUtil.class);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(METALEXER_DEFAULT_COMPONENT_TEMPLATE)));

        return writer.toString();
    }

    private String generateMetaLexerBeaverLayout() throws IOException {
        VelocityContext context = new VelocityContext();
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(METALEXER_BEAVER_LAYOUT_TEMPLATE)));

        return writer.toString();
    }
}
