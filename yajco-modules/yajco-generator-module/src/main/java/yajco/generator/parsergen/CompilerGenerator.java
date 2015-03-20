package yajco.generator.parsergen;

import java.io.File;
import java.util.Properties;
import javax.annotation.processing.Filer;
import yajco.generator.FilesGenerator;
import yajco.model.Language;

public interface CompilerGenerator extends FilesGenerator{

    void generateFiles(Language language, Filer filer, Properties properties, String parserClassName);
    
}
