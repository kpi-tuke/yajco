package tuke.pargen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import yajco.annotation.processor.MapsTo;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@MapsTo(yajco.model.pattern.impl.Parentheses.class)
//Remove default values, have to be specified, or create a better processing with message about LPAR and RPAR
public @interface Parentheses {

	String left() default "(";

	String right() default ")";
}
