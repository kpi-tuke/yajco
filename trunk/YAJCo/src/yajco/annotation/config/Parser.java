package yajco.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface Parser {

	@Deprecated
        String className();

	String mainNode() default "";

	TokenDef[] tokens() default {};

	Skip[] skips() default {};

	Option[] options() default {};
}
