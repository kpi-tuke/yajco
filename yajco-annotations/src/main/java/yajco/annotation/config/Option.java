package yajco.annotation.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Possible values (name - value):
 * generateTools - true = in addition to parser generate visitor, printer and textual yajco representation of language
 * compilerGenerator - "string of class name implementing yajco.generator.parsergen.CompilerGenerator" - use this generator instead of build in generators
 * @author DeeL
 */
@Retention(RetentionPolicy.SOURCE)
//Target: None
public @interface Option {

    String name();

    String value();
}
