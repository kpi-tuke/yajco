package tuke.pargen.annotation.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import yajco.annotation.processor.MapsTo;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@MapsTo(yajco.model.pattern.impl.Identifier.class)
public @interface Identifier {

	/**
	 * XPath expression for definition of the context in which should be marked concept (domain class) unique.
	 */
	String unique() default "";
}
