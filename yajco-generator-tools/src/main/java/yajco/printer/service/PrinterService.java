package yajco.printer.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import yajco.GeneratorHelper;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.model.Language;
import yajco.printer.Printer;


public class PrinterService implements FilesGenerator{
    private static final String PROPERTY_ENABLER = "text";
    
    private static final String FILE_NAME_SUFFIX = ".lang";

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
        String option = properties.getProperty(GeneratorHelper.GENERATE_TOOLS_KEY, "").toLowerCase();
        if ( !option.contains("all") && !option.contains(PROPERTY_ENABLER)) {
            System.out.println(getClass().getCanonicalName()+": Textual language representation not generated - property disabled (set "+GeneratorHelper.GENERATE_TOOLS_KEY+" to '"+PROPERTY_ENABLER+"' or 'all')");
            return;
        } 
        
        Writer writer = null;
        try {
            FileObject fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", language.getName() + FILE_NAME_SUFFIX);
            //File file = new File(directory, language.getName() + FILE_NAME_SUFFIX);
            writer = fileObject.openWriter();
            Printer printer = new Printer();
            printer.printLanguage(new PrintWriter(writer), language);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot print language text file", ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                throw new GeneratorException("Cannot print language text file", ex);
            }
        }
    }
    
}
