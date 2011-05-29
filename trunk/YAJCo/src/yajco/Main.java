package yajco;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.classgen.ClassGenerator;
import yajco.generator.util.GeneratorHelper;
import yajco.printergen.PrettyPrinterGenerator;
import yajco.visitorgen.VisitorGenerator;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.SkipDef;
import yajco.model.TokenDef;
import yajco.model.TokenPart;
import yajco.grammar.bnf.Alternative;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.bnf.Production;
//import yajco.model.parser.LALRLanguageParser;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.NotationPattern;
import yajco.model.pattern.impl.Associativity;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.Parentheses;
import yajco.model.pattern.impl.References;
import yajco.model.translator.YajcoModelToBNFGrammarTranslator;
import yajco.model.type.ArrayType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.PrimitiveTypeConst;
import yajco.model.type.ReferenceType;
import yajco.parser.Parser;
import yajco.parsergen.beaver.BeaverParserGenerator;
import yajco.printer.Printer;
import yajco.refresgen.AspectObjectRegistratorGenerator;

public class Main {
    public static void main(String[] args) throws Exception {
        Parser parser = new Parser();
        //LALRLanguageParser newParser = new LALRLanguageParser();
        Printer printer = new Printer();
        Language language = null;

        System.out.println("--------------------------------------------------------------------------------------------------------");
        //language = newParser.parse(new FileReader("desk.lang"));
        //printer.printLanguage(new PrintWriter(System.out), language);
//        System.out.println("===================================== Visitor class ====================================================");
//        visitorGenerator.generate(language, new PrintWriter(System.out));
//        System.out.println("===================================== Printer class ====================================================");
//        prettyPrinterGenerator.generate(language, new PrintWriter(System.out));
        //System.out.println("==================================* Classes generation *=================================================");
        ////File directory = new File(System.getProperty("user.dir")+"\\src\\test");
        //classGenerator.generate(language, directory);
        //System.out.println("--- DONE ---");
        //System.out.println("==================================* Aspect object registrator generation *=================================================");
        //aspectObjectRegistratorGenerator.generate(language, directory);
        System.out.println("===================================== Generating ====================================================");
        ////new GeneratorHelper(language, directory).generateAll();
        System.out.println("--- DONE ---");

//        System.out.println("--------------------------------------------------------------------------------------------------------");
//        language = parser.parse(new FileReader("expr.lang"));
//        language.getConcept("Number").setParent(language.getConcept("Expression")); // na otestovanie dedenia... kedze aktualne nefunguje akosi
//        System.out.println("===================================== Visitor class ====================================================");
//        visitorGenerator.generate(language, new PrintWriter(System.out));
//
//        System.out.println("===================================== Printer class ====================================================");
//        prettyPrinterGenerator.generate(language, new PrintWriter(System.out));
//
//        System.out.println("--------------------------------------------------------------------------------------------------------");
//        language = parser.parse(new FileReader("desk.lang"));
//        printer.printLanguage(new PrintWriter(System.out), language);
//        System.out.println("===================================== Visitor class ====================================================");
//        visitorGenerator.generate(language, new PrintWriter(System.out));
//        System.out.println("===================================== Printer class ====================================================");
//        prettyPrinterGenerator.generate(language, new PrintWriter(System.out));
//        System.out.println("==================================* Classes generation *=================================================");
//        File directory = new File(System.getProperty("user.dir")+"\\src\\test\\model");
//        classGenerator.generate(language, directory);
//        System.out.println("--- DONE ---");
//
////        System.out.println("--------------------------------------------------------------------------------------------------------");
////        language = parser.parse(new FileReader("expr.lang"));
////        language.getConcept("Number").setParent(language.getConcept("Expression")); // na otestovanie dedenia... kedze aktualne nefunguje akosi
////        System.out.println("===================================== Visitor class ====================================================");
////        visitorGenerator.generate(language, new PrintWriter(System.out));
////
////        System.out.println("===================================== Printer class ====================================================");
////        prettyPrinterGenerator.generate(language, new PrintWriter(System.out));
////
////        System.out.println("--------------------------------------------------------------------------------------------------------");
////        language = parser.parse(new FileReader("states.lang"));
////        printer.printLanguage(new PrintWriter(System.out), language);
////        System.out.println("===================================== Visitor class ====================================================");
////        visitorGenerator.generate(language, new PrintWriter(System.out));
////        System.out.println("===================================== Printer class ====================================================");
////        prettyPrinterGenerator.generate(language, new PrintWriter(System.out));

// VSETKO DALEJ JE RADOV TEST
//		TokenDef valueToken = new TokenDef("VALUE", "[0-9]+");
//		ArrayList<TokenDef> tokens = new ArrayList<TokenDef>();
//		tokens.add(valueToken);
//		Concept expressionConcept = new Concept("Expression", null);
//		expressionConcept.addPattern(new Parentheses());
//
//		Concept numberConcept = new Concept("Number", null);
//		numberConcept.setParent(expressionConcept);
//		numberConcept.addProperty(new Property("value", new PrimitiveType(PrimitiveTypeConst.INTEGER), null));
//		NotationPart part = new PropertyReferencePart(numberConcept.getProperty("value"), null);
//		numberConcept.addNotation(new Notation(new NotationPart[]{part}, new NotationPattern[]{}));
//
//		Concept addConcept = new Concept("Add", null);
//		addConcept.setParent(expressionConcept);
//		addConcept.addPattern(new Operator(1, Associativity.LEFT));
//		addConcept.addProperty(new Property("expression1", new ReferenceType(expressionConcept, null), null));
//		addConcept.addProperty(new Property("expression2", new ReferenceType(expressionConcept, null), null));
//		Notation notation = new Notation(null);
//		part = new PropertyReferencePart(addConcept.getProperty("expression1"), null);
//		notation.addPart(part);
//		part = new TokenPart("+");
//		notation.addPart(part);
//		part = new PropertyReferencePart(addConcept.getProperty("expression2"), null);
//		notation.addPart(part);
//		addConcept.addNotation(notation);
//
//		Concept mulConcept = new Concept("Mul", null);
//		mulConcept.setParent(expressionConcept);
//		mulConcept.addPattern(new Operator(2, Associativity.LEFT));
//		mulConcept.addProperty(new Property("expression1", new ReferenceType(expressionConcept, null), null));
//		mulConcept.addProperty(new Property("expression2", new ReferenceType(expressionConcept, null), null));
//		notation = new Notation(null);
//		part = new PropertyReferencePart(mulConcept.getProperty("expression1"), null);
//		notation.addPart(part);
//		part = new TokenPart("*");
//		notation.addPart(part);
//		part = new PropertyReferencePart(mulConcept.getProperty("expression2"), null);
//		notation.addPart(part);
//		mulConcept.addNotation(notation);
//
//		Concept divConcept = new Concept("Div", null);
//		divConcept.setParent(expressionConcept);
//		divConcept.addPattern(new Operator(2, Associativity.LEFT));
//		divConcept.addProperty(new Property("expression1", new ReferenceType(expressionConcept, null), null));
//		divConcept.addProperty(new Property("expression2", new ReferenceType(expressionConcept, null), null));
//		notation = new Notation(null);
//		part = new PropertyReferencePart(divConcept.getProperty("expression1"), null);
//		notation.addPart(part);
//		part = new TokenPart("/");
//		notation.addPart(part);
//		part = new PropertyReferencePart(divConcept.getProperty("expression2"), null);
//		notation.addPart(part);
//		divConcept.addNotation(notation);
//
//		Concept unaryMinusConcept = new Concept("UnaryMinus", null);
//		unaryMinusConcept.setParent(expressionConcept);
//		unaryMinusConcept.addPattern(new Operator(3, Associativity.RIGHT));
//		unaryMinusConcept.addProperty(new Property("expression", new ReferenceType(expressionConcept, null), null));
//		notation = new Notation(null);
//		part = new TokenPart("-");
//		notation.addPart(part);
//		part = new PropertyReferencePart(unaryMinusConcept.getProperty("expression"), null);
//		notation.addPart(part);
//		unaryMinusConcept.addNotation(notation);
//
//		Concept sumConcept = new Concept("Sum", null);
//		sumConcept.setParent(expressionConcept);
//		sumConcept.addProperty(new Property("values", new ArrayType(new ReferenceType(numberConcept, null)), null));
//		notation = new Notation(null);
//		notation.addPart(new PropertyReferencePart(sumConcept.getProperty("values"), null));
//		sumConcept.addNotation(notation);
//
//		Concept minusConcept = new Concept("Minus", null);
//		minusConcept.setParent(expressionConcept);
//		minusConcept.addPattern(new Operator(1, Associativity.LEFT));
//		minusConcept.addProperty(new Property("expression1", new ReferenceType(expressionConcept, null), null));
//		minusConcept.addProperty(new Property("expression2", new ReferenceType(expressionConcept, null), null));
//		notation = new Notation(null);
//		part = new PropertyReferencePart(minusConcept.getProperty("expression1"), null);
//		notation.addPart(part);
//		part = new TokenPart("-");
//		notation.addPart(part);
//		part = new PropertyReferencePart(minusConcept.getProperty("expression2"), null);
//		notation.addPart(part);
//		minusConcept.addNotation(notation);
//
//		language = new Language(null);
//		language.setTokens(tokens);
//		language.setSkips(new ArrayList<SkipDef>());
//		language.setName("sk.tuke.language.expression");
//		language.addConcept(expressionConcept);
//		language.addConcept(numberConcept);
//		language.addConcept(addConcept);
//		language.addConcept(minusConcept);
//		language.addConcept(mulConcept);
//		language.addConcept(divConcept);
//		language.addConcept(unaryMinusConcept);
////		language.addConcept(sumConcept);
//
//		new Printer().printLanguage(new PrintWriter(System.out), language);
//		System.out.println("----------------------------------------------------------------------------------------");
//		Grammar grammar = YajcoModelToBNFGrammarTranslator.getInstance().translate(language);
//		for (Production p : grammar.getProductions().values()) {
//			System.out.println(p);
//		}
//		System.out.println("----------------------------------------------------------------------------------------");
//		for (Integer priority : grammar.getOperatorPool().keySet()) {
//			for (Alternative a : grammar.getOperatorPool().get(priority)) {
//				System.out.println(a + "(" + priority + ")");
//			}
//		}
// TOTO TU BOLO ZAKOMENTOVANE AJ TAK
//		System.out.println("----------------------------------------------------------------------------------------");
//		for (NonterminalSymbol n : grammar.getNonterminals().values()) {
//			if (n.getPattern(Operator.class) != null) {
//				System.out.println(n);
//			}
//		}
// AZ POTIAL

// RADOV VYPIS
//		System.out.println("----------------------------------------------------------------------------------------");
//		BeaverParserGenerator.getInstance().generateFrom(language, grammar, System.out);
//		System.out.println("----------------------------------------------------------------------------------------");
//		VelocityEngine engine = new VelocityEngine();
//		VelocityContext context = new VelocityContext();
//		context.put("language", language);
//		context.put("terminals", grammar.getTerminals().values());
//		context.put("regexps", grammar.getTerminalPool());
//		context.put("parserName", "LALRExpressionParser");
//		context.put("parserPackage", "sk.tuke.language.expression.parser.beaver");
//		context.put("defaultSymbolName", "SYMBOL");
//		context.put("Utilities", tuke.pargen.javacc.Utilities.class);
//		StringWriter writer = new StringWriter();
//		engine.evaluate(context, writer, "", new InputStreamReader(Main.class.getResourceAsStream("/yajco/parsergen/beaver/templates/BeaverScannerTemplate.vm")));
//		System.out.println(writer.toString());
//		System.out.println("----------------------------------------------------------------------------------------");
	}
}
