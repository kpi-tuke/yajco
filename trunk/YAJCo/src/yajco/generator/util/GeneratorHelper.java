package yajco.generator.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import yajco.generator.GeneratorException;
import yajco.generator.classgen.ClassGenerator;
import yajco.generator.FilesGenerator;
import yajco.model.Language;
import yajco.parser.ParseException;
import yajco.parser.Parser;
import yajco.printer.Printer;
import yajco.generator.printergen.PrettyPrinterGenerator;
import yajco.generator.refresgen.AspectObjectRegistratorGenerator;
import yajco.generator.visitorgen.VisitorGenerator;

public class GeneratorHelper {

    private Language language;
    private File directory;

    public static GeneratorHelper getGeneratorHelper(File file, File directory) throws ParseException, FileNotFoundException {
        Parser parser = new Parser();
        Language language = parser.parse(new FileReader(file));
        return new GeneratorHelper(language, directory);
    }

    public GeneratorHelper(Language language, File directory) {
        if (directory.isDirectory()) {
            this.language = language;
            this.directory = directory;
        } else {
            throw new GeneratorException("Specified directory [" + directory.getAbsolutePath() + "]does not exist!");
        }
    }

    public void generateAll() {
        generateAllExceptModelClassFiles();
        generateModelClassFiles();
    }

    public void generateAllExceptModelClassFiles() {
        generateVisitor();
        generatePrettyPrinter();
        generateReferenceResolverRegistrator();
        //zatial pre test
        generateYAJCoModelTextFile();
    }

    public void generateVisitor() {
        FilesGenerator visitorGenerator = new VisitorGenerator();
        visitorGenerator.generateFiles(language, directory);
    }

    public void generatePrettyPrinter() {
        FilesGenerator prettyPrinterGenerator = new PrettyPrinterGenerator();
        prettyPrinterGenerator.generateFiles(language, directory);
    }

    public void generateModelClassFiles() {
        FilesGenerator classGenerator = new ClassGenerator();
        classGenerator.generateFiles(language, directory);
    }

    public void generateReferenceResolverRegistrator() {
        FilesGenerator registratorGenerator = new AspectObjectRegistratorGenerator();
        registratorGenerator.generateFiles(language, directory);
    }

    public void generateYAJCoModelTextFile() {
        PrintWriter printWriter = null;
        try {
            File file = new File(directory, language.getName() + ".lang");
            printWriter = new PrintWriter(file);
            Printer printer = new Printer();
            printer.printLanguage(printWriter, language);
        } catch (FileNotFoundException ex) {
            throw new GeneratorException("Cannot print language text file", ex);
        } finally {
            printWriter.close();
        }

    }
}
