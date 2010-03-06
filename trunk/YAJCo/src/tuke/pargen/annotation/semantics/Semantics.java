package tuke.pargen.annotation.semantics;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
@Target(ElementType.METHOD)
public @interface Semantics {
    String value() default "";
}
