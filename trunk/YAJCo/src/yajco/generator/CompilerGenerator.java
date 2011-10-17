package yajco.generator;

import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;
import yajco.model.Language;

public interface CompilerGenerator {

    void generateCompilers(ProcessingEnvironment processingEnv, Language language) throws IOException;

}
