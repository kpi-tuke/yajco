package yajco.generator.parsergen.lisa;

import Lisa.Interface.CMessage;
import Lisa.Interface.Message;
import Lisa.Language.CCompiler;
import Lisa.Language.CCompilerProperties;
import Lisa.Language.CLanguage;
import Lisa.Language.CompilerInputManager;
import Lisa.Parser.CProduction;
import Lisa.WebService.CServiceBean;
import Lisa.WebService.CStringCompilerInputManager;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.ReferenceResolver;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.generator.util.Utilities;
import yajco.generator.parsergen.lisa.translator.SemLangToLisaJavaTranslator;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.Symbol;
import yajco.grammar.TerminalSymbol;
import yajco.grammar.bnf.Alternative;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.bnf.Production;
import yajco.grammar.translator.YajcoModelToBNFGrammarTranslator;
import yajco.model.Language;
import yajco.model.SkipDef;

public class LisaCompilerGenerator implements CompilerGenerator {

    private static final String LISA_COMPILER_CLASS_TEMPLATE = "/yajco/generator/parsergen/lisa/templates/ParserClassTemplate.vm";
    private static final String LISA_PARSE_EXCEPTION_CLASS_TEMPLATE = "/yajco/generator/parsergen/lisa/templates/ParseExceptionClassTemplate.vm";
    
    public static final String DEFAULT_PACKAGE_NAME = "parser.lisa";
    public static final String PARSER_CLASS_NAME_PREFIX = "Lisa";
    public static final String PARSER_CLASS_NAME_SUFFIX = "Parser";
    public static final String LISA_SCANNER_CLASS_NAME = "LisaScanner";
    public static final String LISA_PARSER_CLASS_NAME = "LisaParser";
    public static final String LISA_TRANSLATOR_CLASS_NAME = "LisaTranslator";
    public static final String LISA_COMPILER_CLASS_NAME = "MainParser";
    public static final String LISA_SPECIFICATION_FILE_SUFFIX = ".lisa";
    
    private String parserPackageName;
    private Filer filer;
    private Language language;
    private VelocityEngine engine;
    private YajcoModelToBNFGrammarTranslator grammarTranslator = YajcoModelToBNFGrammarTranslator.getInstance();

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties, String parserClassName) {
        throw new UnsupportedOperationException("Not supported to define own parser class name, yet.");
    }
    
    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
//        if (language == null || directory == null) {
//            throw new IllegalArgumentException("language and directory parameters cannot be null");
//        }
//        while (!directory.isDirectory()) {
//            directory = directory.getParentFile();
//            if (directory == null) {
//                throw new GeneratorException("cannot find directory");
//            }
//        }
        this.filer = filer;
        this.language = language;
        if (engine == null) {
            engine = new VelocityEngine();
        }
        generateLisaCompiler();
    }

    private void generateLisaCompiler() {
        try {
            String mainElementClassName = yajco.model.utilities.Utilities.getFullConceptClassName(language, language.getConcepts().get(0));
            parserPackageName = language.getName() != null ? language.getName() + "." + DEFAULT_PACKAGE_NAME : DEFAULT_PACKAGE_NAME;
            String parserClassName = PARSER_CLASS_NAME_PREFIX + language.getConcepts().get(0).getConceptName() + PARSER_CLASS_NAME_SUFFIX;
            
            CCompilerProperties properties = new CCompilerProperties();
            properties.setCompile(false);
            properties.setGenerateCompiler(false);
            properties.setGenerateEvaluator(false);
            properties.setGenerateIncrementalParser(false);
            properties.setParseAbstractSyntax(true);
            properties.setGenerateScannerValue(LISA_SCANNER_CLASS_NAME + ".java");
            properties.setGenerateParserValue(LISA_PARSER_CLASS_NAME + ".java");
            
            String lisaSpecification = getLisaSpecification(language);
            
            System.out.println("---LISA---");
            System.out.println(lisaSpecification);
            String fileName = language.getConcepts().get(0).getConceptName() + LISA_SPECIFICATION_FILE_SUFFIX;
            //File file = Utilities.createFile(filer, parserPackageName, fileName);
            FileObject fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, fileName);
            fileObject.openWriter().append(lisaSpecification).close();
            
            System.out.println("Generating Lisa:");
            CCompiler compiler = new CCompiler();
            compiler.setProperties(properties);
            boolean result = compiler.compile("default.lisa", new CStringCompilerInputManager(lisaSpecification));
            //boolean result = compiler.generateStructures();
            System.out.println("Result: " + result);
            if (!result) {
                throw new GeneratorException("Cannot generate LISA parser");
            }
            CLanguage lisaLanguage = compiler.getLanguage(null);
            //---- LISA SCANNER ----
            fileObject = filer.createClassFile(parserPackageName+"."+LISA_SCANNER_CLASS_NAME);
            //file = Utilities.createFile(filer, parserPackageName, LISA_SCANNER_CLASS_NAME + ".java");
            OutputStream os = fileObject.openOutputStream(); //new FileOutputStream(file);
            String pack = "package " + parserPackageName + ";\n";
            os.write(pack.getBytes());
            compiler.generateScanner(os);
            os.close();

            //---- LISA PARSER ----
            //file = Utilities.createFile(filer, parserPackageName, LISA_PARSER_CLASS_NAME + ".java");
            fileObject = filer.createClassFile(parserPackageName+"."+LISA_PARSER_CLASS_NAME);
            os = fileObject.openOutputStream(); //new FileOutputStream(file);
            os.write(pack.getBytes());
            compiler.generateParser(os);
            os.close();

            //---- LISA YAJCO TRANSLATOR ----
            //file = Utilities.createFile(filer, parserPackageName, LISA_TRANSLATOR_CLASS_NAME + ".java");
            fileObject = filer.createClassFile(parserPackageName+"."+LISA_TRANSLATOR_CLASS_NAME);
            Writer writer = fileObject.openWriter();//new FileWriter(file);
            writer.write(getLisaYajcoTranslator(language, lisaLanguage));
            writer.close();

            //---- LISA YAJCO COMPILER ----
            String parserClassPackageName = parserPackageName.substring(0, parserPackageName.lastIndexOf('.'));
            //file = Utilities.createFile(filer, parserClassPackageName, LISA_PARSER_CLASS_NAME + ".java");
            final String lisaCompilerFQN = parserClassPackageName + "." + LISA_PARSER_CLASS_NAME;
            fileObject = filer.createClassFile(lisaCompilerFQN);
            writer = fileObject.openWriter(); //new FileWriter(file);
            //writer.write(getLisaCompiler(language));
            writer.write(generateLisaCompilerClass(LISA_PARSER_CLASS_NAME, parserPackageName, parserClassPackageName, mainElementClassName, LISA_SCANNER_CLASS_NAME, LISA_TRANSLATOR_CLASS_NAME, ReferenceResolver.class.getCanonicalName()));
            writer.close();
            CompilerGenerator.registerParserServiceProvider(lisaCompilerFQN, filer);

            //----- LISA EXCEPTION CLASS ----
            //file = Utilities.createFile(filer, parserClassPackageName, "LisaParseException.java");
            fileObject = filer.createClassFile(parserClassPackageName+"."+"LisaParseException");
            writer = fileObject.openWriter(); //new FileWriter(file);
            writer.write(generateLisaParseExceptionClass(parserClassPackageName));
            writer.flush();
            writer.close();

            // Print messages from LISA tool
            for (Object s : CMessage.getMessage().getMessages()) {
                Message m = (Message) s;
                System.out.println(m.getMessage());
            }
        } catch (IOException iOException) {
            throw new GeneratorException("Cannot generate LISA parser.", iOException);
        } 
    }

    private String getLisaSpecification(Language language) {
        Grammar grammar = grammarTranslator.translate(language);
        StringBuilder ls = new StringBuilder();
        ls.append("language ").append(getLisaLangName(language)).append(" ");
        ls.append("{");
        ls.append("\n");
        ls.append("lexicon {");
        ls.append("\n");
        Set<TerminalSymbol> terminals = grammar.getTerminalPool().keySet();
        for (TerminalSymbol terminal : terminals) {
            ls.append(getLisaTermName(terminal));
            ls.append("\t");
            String regex = grammar.getTerminalPool().get(terminal);
            if (terminal.getName().startsWith(YajcoModelToBNFGrammarTranslator.DEFAULT_SYMBOL_NAME)) {
                regex = Utilities.encodeStringIntoRegex(regex);
            }
            ls.append(regex);
            ls.append("\n");
        }

        //speci
//        ls.append("ignore [\\0x0D\\0x0A\\ ]\n");
        if (!language.getSkips().isEmpty()) {
            ls.append("ignore");
            ls.append("\t");
            boolean first = true;
            for (SkipDef skip : language.getSkips()) {
                if (!first) {
                    ls.append(" | ");
                }
                first = false;
                ls.append(skip.getRegexp());
            }
            ls.append("\n");
        }
        ls.append("}");
        ls.append("\n");
        //start RULE
        ls.append("rule start {");
        ls.append("\n");
        ls.append("START ::= ").append(getLisaNonTermName(grammar.getStartSymbol())).append(";");
        ls.append("\n");
        ls.append("}");
        ls.append("\n");
        //next production rules
        for (Production production : grammar.getProductions().values()) {
            ls.append("rule ");
            ls.append(getLisaRuleName(production.getLhs()));
            ls.append(" {");
            ls.append("\n");
            ls.append(getLisaNonTermName(production.getLhs()));
            ls.append(" ::= ");
            boolean first = true;
            for (Alternative alternative : production.getRhs()) {
                if (!first) {
                    ls.append("\n");
                    ls.append("| ");
                }
                first = false;
                if (alternative.isEmpty()) {
                    ls.append("epsilon ");
                } else {
                    for (Symbol symbol : alternative.getSymbols()) {
                        printSymbol(symbol, ls);
                        ls.append(" ");
                    }
                }
                ls.append("compute ");
                if (alternative.getPriority() != -1) {
                    ls.append("priority: ").append(alternative.getPriority()).append(" ");
                }
                if (alternative.getAssociativity() != null) {
                    String assoc;
                    switch (alternative.getAssociativity()) {
                        case RIGHT:
                            assoc = "assoc: right ";
                            break;
                        case NONE:
                            assoc = "";
                            break;
                        default:
                            assoc = "assoc: left ";
                    }
                    ls.append(assoc);
                }
                ls.append("{}");
            }
            ls.append(";");
            ls.append("\n");
            ls.append("}");
            ls.append("\n");
        }
        ls.append("}");
        ls.append("\n");

        return ls.toString();
    }

    private String getLisaYajcoTranslator(Language language, CLanguage lisaLang) {
        Grammar grammar = grammarTranslator.translate(language);
        SemLangToLisaJavaTranslator translator = SemLangToLisaJavaTranslator.getInstance();
        translator.setLanguage(language);
        List<Alternative> alternatives = sortAlternatives(grammar, lisaLang);
        StringBuilder sb = new StringBuilder();

        sb.append("package ");
        sb.append(parserPackageName);
        sb.append(";\n");
        sb.append("\n");
        sb.append("public class " + LISA_TRANSLATOR_CLASS_NAME + " {\n");
        sb.append("public ").append(getRootConceptFullClassName(language)).append(" evaluate(Lisa.Parser.CTreeNode node) {\n");
        sb.append("Object result = production(node);\n");
        sb.append("if (result instanceof ").append(getRootConceptFullClassName(language)).append(") {\n");
        sb.append("    return (").append(getRootConceptFullClassName(language)).append(") result;\n");
        sb.append("} else {\n");
        sb.append("    return null; // or throw Excepction\n");
        sb.append("}\n");
        sb.append("}\n\n");
        sb.append("private static Object production(Lisa.Parser.CTreeNode node) {\n");
        sb.append("switch (node.getProdNumber()) {\n");
        sb.append("case 0:");
        sb.append("\n");

        sb.append(getRootConceptFullClassName(language));
        sb.append(" _result = (");
        sb.append(getRootConceptFullClassName(language));
        sb.append(") ");
        sb.append("production(");
        sb.append("node.getNodeAt(");
        sb.append(0);
        sb.append("));");
        sb.append("\n");
        sb.append("return _result;");
        sb.append("\n");

        int prod = 1;
        for (Alternative alternative : alternatives) {
            sb.append("case ");
            sb.append(prod++);
            sb.append(":");
            sb.append("\n");
            sb.append("{");
            sb.append("\n");
            int i = 0;
            for (Symbol symbol : alternative.getSymbols()) {
                String variable = symbol.getVarName();
                if (variable != null) {
                    if (symbol instanceof TerminalSymbol) {
                        sb.append("java.lang.String");
                        sb.append(" ");
                        sb.append(variable);
                        sb.append(" = ");
                        sb.append("node.getNodeAt(");
                        sb.append(i);
                        sb.append(").getToken().value();");
                    } else if (symbol instanceof NonterminalSymbol) {
                        sb.append(translator.typeToString(symbol.getReturnType()));
                        sb.append(" ");
                        sb.append(variable);
                        sb.append(" = ");
                        sb.append("(");
                        sb.append(translator.typeToString(symbol.getReturnType()));
                        sb.append(") ");
                        sb.append("production(");
                        sb.append("node.getNodeAt(");
                        sb.append(i);
                        sb.append("));");
                    }

                    sb.append("\n");
                }
                i++;
            }
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(buff);
            translator.translateActions(alternative.getActions(), language, ps);
            ps.flush();
            sb.append(buff.toString());
            sb.append("\n");
            sb.append("}");
            sb.append("\n");
            sb.append("\n");
        }
        sb.append("}");
        sb.append("\n");
        sb.append("return null;\n");
        sb.append("}");
        sb.append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String getRootConceptFullClassName(Language language) {
        return language.getName() + "." + language.getConcepts().get(0).getName();
    }

    private String getLisaNonTermName(NonterminalSymbol nonterminalSymbol) {
        return nonterminalSymbol.getName().toUpperCase();
    }

    private String getLisaRuleName(NonterminalSymbol nonterminalSymbol) {
        return nonterminalSymbol.getName().toLowerCase() + "_yajcoRule";
    }

    private String getLisaTermName(TerminalSymbol symbol) {
        return symbol.getName().toLowerCase();
    }

    private String getLisaLangName(Language language) {
        return language.getName().replace('.', '_');
    }

    private void printSymbol(Symbol symbol, StringBuilder sb) {
        if (symbol instanceof NonterminalSymbol) {
            NonterminalSymbol nonTerm = (NonterminalSymbol) symbol;
            sb.append(getLisaNonTermName(nonTerm));
        } else if (symbol instanceof TerminalSymbol) {
            TerminalSymbol term = (TerminalSymbol) symbol;
            sb.append("#");
            sb.append(getLisaTermName(term));
        }
    }

    private List<Alternative> sortAlternatives(Grammar grammar, CLanguage lisaLang) {
        ArrayList<Alternative> list = new ArrayList<Alternative>();
        Collection<Production> productions = grammar.getProductions().values();
        if (lisaLang.getBNFProductions() == null) {
            return list;
        }
        for (Object o : lisaLang.getBNFProductions()) {
            CProduction prod = (CProduction) o;
            String name = prod.getLeftSide().getName();
            name = name.substring(name.lastIndexOf("$") + 1);
//            System.out.println("::: name: " + name);
            int size = prod.size();
//            System.out.println("lisaSize: "+ size);
            for (Production production : productions) {
                if (getLisaNonTermName(production.getLhs()).equals(name)) {
//                    System.out.println("myName: "+getLisaNonTermName(production.getLhs()));
                    for (Alternative alternative : production.getRhs()) {
                        // specialny pripad ked je epsilon pouzity
                        if (size == 1 && alternative.isEmpty() && "epsilon".equals(prod.elementAt(0).getName())) {
                            list.add(alternative);
                            continue;
                        }
                        // ostatne pripady
//                        System.out.println("mySize: "+alternative.getSymbols().size());
                        if (size == alternative.getSymbols().size()) {
                            boolean ok = true;
                            for (int i = 0; i < alternative.getSymbols().size(); i++) {
                                Symbol symbol = alternative.getSymbols().get(i);
                                String myTermName = "";
                                if (symbol instanceof NonterminalSymbol) {
                                    NonterminalSymbol nts = (NonterminalSymbol) symbol;
                                    myTermName = getLisaNonTermName(nts);
                                } else if (symbol instanceof TerminalSymbol) {
                                    TerminalSymbol ts = (TerminalSymbol) symbol;
                                    myTermName = getLisaTermName(ts);
                                }
                                String lisaTermName = prod.elementAt(i).getName();
                                lisaTermName = lisaTermName.substring(lisaTermName.lastIndexOf("$") + 1);
//                                System.out.println("Porovnavam:");
//                                System.out.println("   >  "+lisaTermName);
//                                System.out.println("   >> "+myTermName);
                                if (!lisaTermName.equals(myTermName)) {
                                    ok = false;
                                }
                            }
                            if (ok) {
                                list.add(alternative);
                            }
                        }
                    }
                }
            }

        }

        return list;
    }

    private String generateLisaCompilerClass(String parserClassName, String parserPackageName, String parserClassPackageName, String mainElementClassName, String scannerClassName, String translatorClassName, String referenceResolverClassName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserClassName", parserClassName);
        context.put("parserPackageName", parserPackageName);
        context.put("parserClassPackageName", parserClassPackageName);
        context.put("mainElementClassName", mainElementClassName);
        context.put("scannerClassName", scannerClassName);
        context.put("referenceResolverClassName", referenceResolverClassName);
        context.put("translatorClassName", translatorClassName);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(LISA_COMPILER_CLASS_TEMPLATE)));

        return writer.toString();
    }

    private String generateLisaParseExceptionClass(String parserClassPackageName) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("parserClassPackageName", parserClassPackageName);
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream(LISA_PARSE_EXCEPTION_CLASS_TEMPLATE)));

        return writer.toString();
    }
}
