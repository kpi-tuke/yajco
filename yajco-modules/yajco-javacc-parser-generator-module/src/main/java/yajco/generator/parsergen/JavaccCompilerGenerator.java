package yajco.generator.parsergen;

import java.io.File;
import java.util.Properties;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.javacc.JavaCCParserGenerator;
import yajco.model.Language;

public class JavaccCompilerGenerator implements CompilerGenerator {

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
        generateFiles(language, filer, properties, null);
    }

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties, String parserClassName) {
        if (language == null || filer == null) {
            throw new IllegalArgumentException("language and filer cannot be null");
        }
        JavaCCParserGenerator generator = new JavaCCParserGenerator(language, filer, parserClassName);
        generator.generate();
    }

}
