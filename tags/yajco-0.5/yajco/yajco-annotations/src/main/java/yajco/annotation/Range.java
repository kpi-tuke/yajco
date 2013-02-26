package yajco.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import yajco.annotation.processor.MapsTo;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
@MapsTo("yajco.model.pattern.impl.Range")
public @interface Range {

	public static final int INFINITY = -1;

	int minOccurs() default 0;

	int maxOccurs() default INFINITY;
}
