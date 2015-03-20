package yajco.annotation.reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import yajco.annotation.processor.MapsTo;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface References {

	/**
	 * Class of a referenced element.
	 */
	Class value();

	/**
	 * Field name.
	 * TODO: use name of the parameter if it is not supplied.
	 */
	String field() default "";

	/**
	 * XPath expression for finding the declared concept node.
	 */
	String path() default "";

	/**
	 * Automatically create concept node when the searched node not exists.
	 */
	boolean create() default false;
}
