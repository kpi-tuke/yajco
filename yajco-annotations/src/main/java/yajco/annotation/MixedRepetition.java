package yajco.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mixing elements from multiple collection parameters in concrete syntax.
 *
 * When applied to a constructor/factory method with multiple collection (List/array) parameters,
 * this annotation allows interleaving elements from different collections in the concrete syntax.
 * During parsing, elements are automatically sorted into their corresponding collections based on type.
 *
 * Example:
 * <pre>
 * @MixedRepetition
 * public Controller(List<Event> events, List<Command> commands, List<Variable> variables) {
 *     // Instead of requiring all events first, then all commands, then all variables,
 *     // the parser accepts them in any mixed order: Event Command Event Variable Command Event
 *     // and automatically distributes them to the correct lists.
 * }
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface MixedRepetition {
    /**
     * Names of parameters to exclude from mixed repetition handling.
     * Excluded parameters will maintain their original position in the grammar.
     */
    String[] exclude() default {};
}
