package yajco.annotation;

import yajco.annotation.processor.MapsTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures how boolean values are represented in the concrete syntax.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
@MapsTo("yajco.model.pattern.impl.BooleanValue")
public @interface BooleanValue {
    String[] trueToken() default {"true"};
    String[] falseToken() default {"false"};
}
