package yajco.generator.util;

import java.io.File;
import tuke.pargen.GeneratorException;
import yajco.classgen.ClassGenerator;
import yajco.model.Language;
import yajco.printergen.PrettyPrinterGenerator;
import yajco.refresgen.AspectObjectRegistratorGenerator;
import yajco.visitorgen.VisitorGenerator;

public class GeneratorHelper {

    private Language language;
    private File directory;

    public GeneratorHelper(Language language, File directory) {
        if (directory.isDirectory()) {
            this.language = language;
            this.directory = directory;
        } else {
            throw new GeneratorException("Specified directory ["+directory.getAbsolutePath()+"]does not exist!");
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
    }

    public void generateVisitor() {
        String visitorPackage = "visitor";
        String visitorClassFileName = "Visitor.java";
        String filePath = Utilities.getLanguagePackageName(language).replace('.', File.separatorChar) + File.separator + visitorPackage + File.separator + visitorClassFileName;
        File file = new File(directory, filePath);
        Utilities.createDirectories(file, false);

        VisitorGenerator visitorGenerator = new VisitorGenerator();
        visitorGenerator.generate(language, file);
    }

    public void generatePrettyPrinter() {
        String printerPackage = "printer";
        String printerClassFileName = "Printer.java";
        String filePath = Utilities.getLanguagePackageName(language).replace('.', File.separatorChar) + File.separator + printerPackage + File.separator + printerClassFileName;
        File file = new File(directory, filePath);
        Utilities.createDirectories(file, false);

        PrettyPrinterGenerator prettyPrinterGenerator = new PrettyPrinterGenerator();
        prettyPrinterGenerator.generate(language, file);
    }

    public void generateModelClassFiles() {
        ClassGenerator classGenerator = new ClassGenerator();
        classGenerator.generate(language, directory);
    }

    public void generateReferenceResolverRegistrator() {
        AspectObjectRegistratorGenerator registratorGenerator = new AspectObjectRegistratorGenerator();
        registratorGenerator.generate(language, directory);
    }
}
