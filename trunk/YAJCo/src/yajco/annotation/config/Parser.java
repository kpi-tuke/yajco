package yajco.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import yajco.annotation.config.parsers.ParserType;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface Parser {

        String className() default "";

	String mainNode() default "";

	TokenDef[] tokens() default {};

	Skip[] skips() default {};

	Option[] options() default {};
        
        ParserType parserType() default ParserType.DEFAULT;
        
}
