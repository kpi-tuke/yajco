package yajco;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import yajco.classgen.ClassGenerator;
import yajco.generator.util.GeneratorHelper;
import yajco.printergen.PrettyPrinterGenerator;
import yajco.visitorgen.VisitorGenerator;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.impl.References;
import yajco.parser.Parser;
import yajco.printer.Printer;
import yajco.refresgen.AspectObjectRegistratorGenerator;

public class Main {
    public static void main(String[] args) throws Exception {
        Parser parser = new Parser();
        Printer printer = new Printer();
        Language language;

        VisitorGenerator visitorGenerator = new VisitorGenerator();
        PrettyPrinterGenerator prettyPrinterGenerator = new PrettyPrinterGenerator();
        ClassGenerator classGenerator = new ClassGenerator();
        AspectObjectRegistratorGenerator aspectObjectRegistratorGenerator = new AspectObjectRegistratorGenerator();

        System.out.println("--------------------------------------------------------------------------------------------------------");
        language = parser.parse(new FileReader("desk.lang"));
        printer.printLanguage(new PrintWriter(System.out), language);
//        System.out.println("===================================== Visitor class ====================================================");
//        visitorGenerator.generate(language, new PrintWriter(System.out));
//        System.out.println("===================================== Printer class ====================================================");
//        prettyPrinterGenerator.generate(language, new PrintWriter(System.out));
        //System.out.println("==================================* Classes generation *=================================================");
        File directory = new File(System.getProperty("user.dir")+"\\src\\test");
        //classGenerator.generate(language, directory);
        //System.out.println("--- DONE ---");
        //System.out.println("==================================* Aspect object registrator generation *=================================================");
        //aspectObjectRegistratorGenerator.generate(language, directory);
        System.out.println("===================================== Generating ====================================================");
        new GeneratorHelper(language, directory).generateAll();
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
//        language = parser.parse(new FileReader("states.lang"));
//        printer.printLanguage(new PrintWriter(System.out), language);
//        System.out.println("===================================== Visitor class ====================================================");
//        visitorGenerator.generate(language, new PrintWriter(System.out));
//        System.out.println("===================================== Printer class ====================================================");
//        prettyPrinterGenerator.generate(language, new PrintWriter(System.out));
    }
}
