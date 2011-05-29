package yajco.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.security.Permission;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.ReferenceResolver;
import yajco.generator.util.Utilities;
import yajco.model.Language;
import yajco.grammar.bnf.Grammar;
import yajco.model.translator.YajcoModelToBNFGrammarTranslator;
import yajco.parsergen.beaver.BeaverParserGenerator;

public class CompilersGenerator {

	static class SimpleExitSecurityManager extends SecurityManager {

		@Override
		public void checkPermission(Permission perm) {
		}

		@Override
		public void checkPermission(Permission perm, Object context) {
		}

		@Override
		public void checkExit(int status) {
			throw new SecurityException();
		}
	}
	private static final String BEAVER_SCANNER_CLASS_TEMPLATE = "/yajco/parsergen/beaver/templates/BeaverScannerTemplate.vm";
	private static final String BEAVER_PARSER_CLASS_TEMPLATE = "/yajco/parsergen/beaver/templates/LALRParserClassTemplate.vm";
	private static final String BEAVER_PARSE_EXCEPTION_CLASS_TEMPLATE = "/yajco/parsergen/beaver/templates/LALRParseExceptionClassTemplate.vm";
	private static final String SYMBOL_LIST_IMPL_CLASS_TEMPLATE = "/yajco/parsergen/beaver/templates/SymbolListImplClassTemplate.vm";
        private static final String SYMBOL_WRAPPER_CLASS_TEMPLATE = "/yajco/parsergen/beaver/templates/SymbolWrapperClassTemplate.vm";
	private static final BeaverParserGenerator beaverParGen = BeaverParserGenerator.getInstance();
	private static final YajcoModelToBNFGrammarTranslator modelToBNFGrammarTranslator = YajcoModelToBNFGrammarTranslator.getInstance();
	private static final CompilersGenerator instance = new CompilersGenerator();
	private ProcessingEnvironment processingEnv;
	private Language language;
	private VelocityEngine engine;

	private CompilersGenerator() {
	}

	public void generateCompilers(ProcessingEnvironment processingEnv, Language language) throws IOException {
		this.processingEnv = processingEnv;
		this.language = language;
		if (engine == null) {
			engine = new VelocityEngine();
		}

		generateBeaverCompiler();
	}

	private void generateBeaverCompiler() throws IOException {
		String mainElementClassName = Utilities.getFullConceptClassName(language, language.getConcepts().get(0));
		Grammar grammar = modelToBNFGrammarTranslator.translate(language);
		String parserPackageName = language.getName() != null ? language.getName() + "." + BeaverParserGenerator.DEFAULT_PACKAGE_NAME : BeaverParserGenerator.DEFAULT_PACKAGE_NAME;
		String parserClassName = BeaverParserGenerator.PARSER_CLASS_NAME_PREFIX + grammar.getStartSymbol().getName() + BeaverParserGenerator.PARSER_CLASS_NAME_SUFFIX;

		// lex. analyzator
		String scannerClassName = parserClassName + "Scanner";
		FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, scannerClassName + ".java");
		Writer writer = fo.openWriter();
		writer.write(generateBeaverScannerClass(grammar, parserClassName, parserPackageName));
		writer.flush();
		writer.close();

		// hlavna trieda parsera
		String parserClassPackageName = parserPackageName.substring(0, parserPackageName.lastIndexOf('.'));
		fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserClassPackageName, parserClassName + ".java");
		writer = fo.openWriter();
		writer.write(generateBeaverParserClass(parserClassName, parserPackageName, parserClassPackageName, mainElementClassName, scannerClassName, ReferenceResolver.class.getCanonicalName()));
		writer.flush();
		writer.close();

		// trieda vynimky parsera
		fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserClassPackageName, "LALRParseException.java");
		writer = fo.openWriter();
		writer.write(generateBeaverParseExceptionClass(parserClassPackageName));
		writer.flush();
		writer.close();

		// trieda SymbolListImpl
		fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, "SymbolListImpl.java");
		writer = fo.openWriter();
		writer.write(generateSymbolListImplClass(parserPackageName));
		writer.flush();
		writer.close();

                // trieda SymbolWrapper
		fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, "SymbolWrapper.java");
		writer = fo.openWriter();
		writer.write(generateSymbolWrapperClass(parserPackageName));
		writer.flush();
		writer.close();

		// vstupny subor pre Beaver
		fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, "grammar.grammar");
		PrintStream streamWriter = new PrintStream(fo.openOutputStream());
		beaverParGen.generateFrom(language, grammar, streamWriter);
		streamWriter.flush();
		streamWriter.close();

		// nastavenie SecurityManagera
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SimpleExitSecurityManager());
		}

		// pouzitie Beaver-a
		URI inputFileURI = fo.toUri();
		File inputFile = inputFileURI.isAbsolute() ? new File(inputFileURI) : new File(inputFileURI.toString());
		try {
			beaver.comp.run.Make.main(new String[]{inputFile.toString()});
		} catch (SecurityException e) {
		}
	}

	private String generateBeaverScannerClass(Grammar grammar, String parserClassName, String parserPackageName) throws IOException {
		VelocityContext context = new VelocityContext();
		context.put("language", language);
		context.put("terminals", grammar.getTerminals().values());
		context.put("regexps", grammar.getTerminalPool());
		context.put("parserName", parserClassName);
		context.put("parserPackage", parserPackageName);
		context.put("defaultSymbolName", YajcoModelToBNFGrammarTranslator.DEFAULT_SYMBOL_NAME);
		context.put("Utilities", yajco.parsergen.javacc.Utilities.class);
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
		engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(BEAVER_PARSER_CLASS_TEMPLATE)));

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

        private String generateSymbolWrapperClass(String parserPackageName) throws IOException {
		VelocityContext context = new VelocityContext();
		context.put("parserPackageName", parserPackageName);
		StringWriter writer = new StringWriter();
		engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(SYMBOL_WRAPPER_CLASS_TEMPLATE)));

		return writer.toString();
	}

	public static CompilersGenerator getInstance() {
		return instance;
	}
}
