package yajco.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares language metadata for editor tooling generation.
 *
 * <p>Place on the same element as {@link Parser} (package-info.java or type).
 * When present, IR generation is automatically enabled — there is no need to
 * add {@code @Option(name = "yajco.generateTools", value = "ir")}.
 *
 * <p>Comment syntax specified here is used both as editor metadata
 * (comment toggling, TextMate scopes) and as a parser skip rule —
 * a separate {@link Skip} for comments is not required.
 *
 * <h3>Example usage:</h3>
 * <pre>
 * &#64;Language(
 *     name = "simple-robot",
 *     description = "A simple robot programming language",
 *     version = "1.0.0",
 *     fileExtensions = {".robot"},
 *     lineComment = "//"
 * )
 * &#64;Parser(
 *     mainNode = "Robot",
 *     skips = { &#64;Skip("\\s") }
 * )
 * package yajco.robot.model;
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface Language {

    /**
     * Language name used as an identifier in generated artifacts.
     * For example: "simple-robot", "math-expressions".
     */
    String name() default "";

    /**
     * Human-readable description of the language.
     * Used in generated README files, package.json, and tree-sitter metadata.
     */
    String description() default "";

    /**
     * Language version string (e.g. "1.0.0").
     * Used in generated package.json and tree-sitter metadata.
     */
    String version() default "";

    /**
     * File extensions associated with this language (e.g. ".robot", ".expr").
     * Each extension should include the leading dot.
     */
    String[] fileExtensions() default {};

    /**
     * Line comment prefix (e.g. "//", "#", "--").
     * When set, a skip rule for this comment style is automatically
     * added to the parser and the IR's comment metadata is populated.
     */
    String lineComment() default "";

    /**
     * Block comment delimiters as a two-element array: {start, end}.
     * For example: {"/*", "*&#47;"}.
     * When set, a skip rule for block comments is automatically
     * added to the parser and the IR's comment metadata is populated.
     */
    String[] blockComment() default {};
}
