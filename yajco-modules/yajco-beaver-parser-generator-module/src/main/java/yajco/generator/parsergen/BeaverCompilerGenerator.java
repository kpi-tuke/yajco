package yajco.generator.parsergen;

import beaver.comp.run.Options;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.ReferenceResolver;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.beaver.BeaverParserGenerator;
import yajco.generator.util.RegexUtil;
import yajco.grammar.TerminalSymbol;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.translator.YajcoModelToBNFGrammarTranslator;
import yajco.model.Language;

public class BeaverCompilerGenerator implements CompilerGenerator {

    private static final String BEAVER_SCANNER_CLASS_TEMPLATE = "/yajco/generator/parsergen/beaver/templates/BeaverScannerTemplate.vm";
    private static final String BEAVER_PARSER_CLASS_TEMPLATE = "/yajco/generator/parsergen/beaver/templates/LALRParserClassTemplate.vm";
    private static final String BEAVER_PARSER_METALEXER_CLASS_TEMPLATE = "/yajco/generator/parsergen/beaver/templates/LALRParserClassMetaLexerTemplate.vm";
    private static final String BEAVER_PARSE_EXCEPTION_CLASS_TEMPLATE = "/yajco/generator/parsergen/beaver/templates/LALRParseExceptionClassTemplate.vm";
    private static final String SYMBOL_LIST_IMPL_CLASS_TEMPLATE = "/yajco/generator/parsergen/beaver/templates/SymbolListImplClassTemplate.vm";
    private static final String SYMBOL_LIST_IMPL_WITH_SHARED_CLASS_TEMPLATE = "/yajco/generator/parsergen/beaver/templates/SymbolListImplWithSharedClassTemplate.vm";
    private static final String SYMBOL_WRAPPER_CLASS_TEMPLATE = "/yajco/generator/parsergen/beaver/templates/SymbolWrapperClassTemplate.vm";
    private static final BeaverParserGenerator beaverParGen = BeaverParserGenerator.getInstance();
    private static final YajcoModelToBNFGrammarTranslator modelToBNFGrammarTranslator = YajcoModelToBNFGrammarTranslator.getInstance();
    private Filer filer;
    private Language language;
    private VelocityEngine engine;
    private boolean metalexerScanner = false;
    private String providedParserClassName = null;

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
        if (language == null || filer == null) {
            throw new IllegalArgumentException("language and filer parameter cannot be null");
        }

        this.filer = filer;
        this.language = language;
        if (engine == null) {
            engine = new VelocityEngine();
        }

        try {
            generateBeaverCompiler();
        } catch (IOException iOException) {
            throw new GeneratorException("Cannot generate Beaver parser ("+iOException.getMessage()+")",iOException);
        }
    }

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties, String parserClassName) {
        this.providedParserClassName = parserClassName;
        generateFiles(language, filer, properties);
    }

    private void generateBeaverCompiler() throws IOException {
        String mainElementClassName = yajco.model.utilities.Utilities.getFullConceptClassName(language, language.getConcepts().get(0));
        Grammar grammar = modelToBNFGrammarTranslator.translate(language);
        String parserPackageName;
        String parserClassName;
        String parserClassPackageName;
        if (providedParserClassName != null && !providedParserClassName.isEmpty()) {
            parserClassPackageName = providedParserClassName.substring(0, providedParserClassName.lastIndexOf("."));
            parserPackageName = parserClassPackageName + ".beaver";
            parserClassName = providedParserClassName.substring(providedParserClassName.lastIndexOf(".")+1);
            
        } else {
            parserPackageName = language.getName() != null ? language.getName() + "." + BeaverParserGenerator.DEFAULT_PACKAGE_NAME : BeaverParserGenerator.DEFAULT_PACKAGE_NAME;
            parserClassName = BeaverParserGenerator.PARSER_CLASS_NAME_PREFIX + grammar.getStartSymbol().getName() + BeaverParserGenerator.PARSER_CLASS_NAME_SUFFIX;
            parserClassPackageName = parserPackageName.substring(0, parserPackageName.lastIndexOf('.'));
        }

        FileObject fileObject;
        Writer writer;

        String scannerClassName;
        if (metalexerScanner) {
            // MetaLexer scanner
            
//            scannerClassName = parserClassName + "MetaLexerScanner";
//            MetaLexerScannerGenerator metalexerGenerator = new MetaLexerScannerGenerator();
//            metalexerGenerator.generateScanner(processingEnv, language, grammar, parserClassName, parserPackageName);
            throw new UnsupportedOperationException("Metalexer Scanner not available!");
        } else {
            // lex. analyzator
            scannerClassName = parserClassName + "Scanner";
            //file = Utilities.createFile(filer, parserPackageName, scannerClassName+".java");
            fileObject = filer.createSourceFile(parserPackageName+"."+scannerClassName);
            writer = fileObject.openWriter(); //new FileWriter(file);
            writer.write(generateBeaverScannerClass(grammar, parserClassName, parserPackageName));
            writer.flush();
            writer.close();
        }

        // hlavna trieda parsera
        //file = Utilities.createFile(filer, parserClassPackageName, parserClassName + ".java");
        fileObject = filer.createSourceFile(parserClassPackageName + "." + parserClassName);
        writer = fileObject.openWriter(); //new FileWriter(file);
        writer.write(generateBeaverParserClass(parserClassName, parserPackageName, parserClassPackageName, mainElementClassName, scannerClassName, ReferenceResolver.class.getCanonicalName()));
        writer.flush();
        writer.close();

        // trieda vynimky parsera
        //file = Utilities.createFile(filer, parserClassPackageName, "LALRParseException.java");
        fileObject = filer.createSourceFile(parserClassPackageName + "." + "ParseException");
        writer = fileObject.openWriter(); //new FileWriter(file);
        writer.write(generateBeaverParseExceptionClass(parserClassPackageName));
        writer.flush();
        writer.close();

        // trieda SymbolListImpl
        //file = Utilities.createFile(filer, parserPackageName, "SymbolListImpl.java");
        fileObject = filer.createSourceFile(parserPackageName + "." + "SymbolListImpl");
        writer = fileObject.openWriter(); //new FileWriter(file);
        writer.write(generateSymbolListImplClass(parserPackageName));
        writer.flush();
        writer.close();

        // trieda SymbolListImplWithShared
        //file = Utilities.createFile(filer, parserPackageName, "SymbolListImplWithShared.java");
        fileObject = filer.createSourceFile(parserPackageName + "." + "SymbolListImplWithShared");
        writer = fileObject.openWriter(); //new FileWriter(file);
        writer.write(generateSymbolListImplWithSharedClass(parserPackageName));
        writer.flush();
        writer.close();

        // trieda SymbolWrapper
        //file = Utilities.createFile(filer, parserPackageName, "SymbolWrapper.java");
        fileObject = filer.createSourceFile(parserPackageName + "." + "SymbolWrapper");
        writer = fileObject.openWriter(); //new FileWriter(file);
        writer.write(generateSymbolWrapperClass(parserPackageName));
        writer.flush();
        writer.close();

        // vstupny subor pre Beaver
        //file = Utilities.createFile(filer, parserPackageName, "grammar.grammar");
        fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, "grammar.grammar");
        PrintStream streamWriter = new PrintStream(fileObject.openOutputStream()); // new PrintStream(file);
        beaverParGen.generateFrom(language, grammar, parserPackageName, parserClassName, streamWriter);
        streamWriter.flush();
        streamWriter.close();

        // remember path to grammar.grammar
        URI inputFileURI = fileObject.toUri();
        try {
            inputFileURI = new URI("file:///").resolve(inputFileURI);
        } catch (URISyntaxException e) {
            System.err.println("Cannot make absolute uri!");
        }

        // registering file later generated by Beaver for compilation
        fileObject = filer.createSourceFile(parserPackageName+"."+parserClassName);
        fileObject.openWriter().close();
        
        // pouzitie Beaver-a
        File inputFile = inputFileURI.isAbsolute() ? new File(inputFileURI) : new File(inputFileURI.toString());
        beaver.comp.util.Log log = new beaver.comp.util.Log();
        beaver.comp.io.SrcReader srcReader = new beaver.comp.io.SrcReader(inputFile);
        try {
            beaver.comp.ParserGenerator.compile(srcReader, new Options(), log);
            log.report(inputFile.getName(), srcReader);
            if (log.hasErrors()) {
                System.err.println("There were errors during Beaver compiler generator");
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private String generateBeaverScannerClass(Grammar grammar, String parserClassName, String parserPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("language", language);
        context.put("terminals", getOrderedAndUsedTerminalSymbols(grammar));
        context.put("regexps", grammar.getTerminalPool());
        context.put("parserName", parserClassName);
        context.put("parserPackage", parserPackageName);
        context.put("defaultSymbolName", YajcoModelToBNFGrammarTranslator.DEFAULT_SYMBOL_NAME);
        context.put("Utilities", yajco.generator.util.Utilities.class);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(BEAVER_SCANNER_CLASS_TEMPLATE)));

        return writer.toString();
    }

    private String generateBeaverParserClass(String parserClassName, String parserPackageName, String parserClassPackageName, String mainElementClassName, String scannerClassName, String referenceResolverClassName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserClassName", parserClassName);
        context.put("parserPackageName", parserPackageName);
        context.put("parserClassPackageName", parserClassPackageName);
        context.put("mainElementClassName", mainElementClassName);
        context.put("scannerClassName", scannerClassName);
        context.put("referenceResolverClassName", referenceResolverClassName);
        StringWriter writer = new StringWriter();
        if (metalexerScanner) {
            engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(BEAVER_PARSER_METALEXER_CLASS_TEMPLATE)));
        } else {
            engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(BEAVER_PARSER_CLASS_TEMPLATE)));
        }

        return writer.toString();
    }

    private String generateBeaverParseExceptionClass(String parserClassPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserClassPackageName", parserClassPackageName);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(BEAVER_PARSE_EXCEPTION_CLASS_TEMPLATE)));

        return writer.toString();
    }

    private String generateSymbolListImplClass(String parserPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserPackageName", parserPackageName);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(SYMBOL_LIST_IMPL_CLASS_TEMPLATE)));

        return writer.toString();
    }

    private String generateSymbolListImplWithSharedClass(String parserPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserPackageName", parserPackageName);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(SYMBOL_LIST_IMPL_WITH_SHARED_CLASS_TEMPLATE)));

        return writer.toString();
    }

    private String generateSymbolWrapperClass(String parserPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserPackageName", parserPackageName);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(SYMBOL_WRAPPER_CLASS_TEMPLATE)));

        return writer.toString();
    }
    
    
    private Set<TerminalSymbol> getOrderedAndUsedTerminalSymbols(Grammar grammar) {
        Set<TerminalSymbol> usedTerminals = BeaverParserGenerator.getUsedTerminals(grammar);
        Set<TerminalSymbol> acyclicTerminals = new LinkedHashSet<TerminalSymbol>();
        Set<TerminalSymbol> cyclicTerminals = new LinkedHashSet<TerminalSymbol>();
        Map<TerminalSymbol, String> terminalPool = grammar.getTerminalPool();
        //order them - move cyclic to last
        for (TerminalSymbol terminalSymbol : usedTerminals) {
            if (RegexUtil.isCyclic(terminalPool.get(terminalSymbol))) {
               cyclicTerminals.add(terminalSymbol); 
            } else {
               acyclicTerminals.add(terminalSymbol);
            }
        }
        acyclicTerminals.addAll(cyclicTerminals);
        return acyclicTerminals;
    }

//    public static BeaverCompilerGenerator getInstance() {
//        return instance;
//    }
}
