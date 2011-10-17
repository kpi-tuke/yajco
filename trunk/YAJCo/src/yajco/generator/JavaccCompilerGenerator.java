package yajco.generator;

import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;
import yajco.generator.parsergen.javacc.JavaCCParserGenerator;
import yajco.model.Language;

public class JavaccCompilerGenerator implements CompilerGenerator {

    public void generateCompilers(ProcessingEnvironment processingEnv, Language language) throws IOException {
        JavaCCParserGenerator generator = new JavaCCParserGenerator(processingEnv, language);
        generator.generate();
    }

}
