package yajco.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a boolean constructor parameter as a flag.
 * The specified token is made optional in the grammar - its presence indicates true, absence indicates false.
 *
 * Example:
 * <pre>
 * public MyClass(@Flag("final") boolean isFinal, String name) { ... }
 * </pre>
 *
 * Generated grammar accepts both:
 * - "final myName" (isFinal = true)
 * - "myName" (isFinal = false)
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface Flag {
    /**
     * The token that represents the flag.
     * When present in input, the boolean parameter receives true; when absent, false.
     */
    String value();
}
