package yajco.annotation.config.parsers;

import yajco.generator.parsergen.BeaverCompilerGenerator;
import yajco.generator.parsergen.JavaccCompilerGenerator;

public enum ParserType {
    DEFAULT(null),
    JAVACC(JavaccCompilerGenerator.class),
    BEAVER(BeaverCompilerGenerator.class);

    private Class compilerGeneratorClass;
    
    private ParserType(Class compilerGeneratorClass) {
        this.compilerGeneratorClass = compilerGeneratorClass;
    }

    public Class getCompilerGeneratorClass() {
        return compilerGeneratorClass;
    }
    
}
