package yajco.annotation;

import yajco.annotation.processor.MapsTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PARAMETER})
@MapsTo("yajco.model.pattern.impl.UniqueValues")
public @interface UniqueValues {
}
