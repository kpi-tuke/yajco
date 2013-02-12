package yajco;

import java.util.Properties;
import javax.annotation.processing.Filer;
import yajco.generator.FilesGenerator;
import yajco.generator.classgen.ClassGenerator;
import yajco.generator.printergen.PrettyPrinterGenerator;
import yajco.generator.visitorgen.VisitorGenerator;
import yajco.model.Language;
import yajco.printer.service.PrinterService;

public class GeneratorHelper {
    
    public static final String GENERATE_TOOLS_KEY = "yajco.generateTools";

    private Language language;
    private Filer filer;
    private Properties properties;

//    public static GeneratorHelper getGeneratorHelper(File file, File directory) throws ParseException, FileNotFoundException {
//        Parser parser = new Parser();
//        Language language = parser.parse(new FileReader(file));
//        return new GeneratorHelper(language, directory);
//    }
    public GeneratorHelper(Language language, Filer filer, Properties properties) {
        this.language = language;
        this.filer = filer;
        this.properties = properties;

    }

    public void generateAll() {
        generateAllExceptModelClassFiles();
        generateModelClassFiles();
    }

    public void generateAllExceptModelClassFiles() {
        generateVisitor();
        generatePrettyPrinter();
        //generateReferenceResolverRegistrator();
        //zatial pre test
        generateYAJCoModelTextFile();
    }

    public void generateVisitor() {
        FilesGenerator visitorGenerator = new VisitorGenerator();
        visitorGenerator.generateFiles(language, filer, properties);
    }

    public void generatePrettyPrinter() {
        FilesGenerator prettyPrinterGenerator = new PrettyPrinterGenerator();
        prettyPrinterGenerator.generateFiles(language, filer, properties);
    }

    public void generateModelClassFiles() {
        FilesGenerator classGenerator = new ClassGenerator();
        classGenerator.generateFiles(language, filer, properties);
    }

//    public void generateReferenceResolverRegistrator() {
//        FilesGenerator registratorGenerator = new AspectObjectRegistratorGenerator();
//        registratorGenerator.generateFiles(language, filer, properties);
//    }

    public void generateYAJCoModelTextFile() {
        PrinterService printerService = new PrinterService();
        printerService.generateFiles(language, filer, properties);
    }
}
