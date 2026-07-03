package yajco.annotation.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Declares a parser skip rule.
 *
 * <p>{@code @Skip} entries are used inside {@link Parser#skips()} to define
 * ignored text such as whitespace and comments.
 *
 * <p>Comment-oriented forms ({@link #lineComment()} and {@link #blockComment()})
 * are the preferred API. They both:
 * <ul>
 *   <li>generate parser skip regular expressions</li>
 *   <li>provide metadata for editors and IDEs</li>
 * </ul>
 *
 * <p>Evaluation priority inside one {@code @Skip} entry:
 * {@link #value()} &gt; {@link #whitespace()} &gt; {@link #lineComment()}
 * &gt; {@link #blockComment()} &gt; deprecated {@link #start()}/{@link #end()}.
 */
@Retention(RetentionPolicy.SOURCE)
//Target: None
public @interface Skip {
    /**
     * Raw regular expression to skip.
     *
     * <p>Use this when shorthand options are insufficient.
     * Example: {@code @Skip("//.*")}.
     */
    String value() default "";

    /**
     * Shorthand for skipping whitespace ({@code \s}).
     * Example: {@code @Skip(whitespace = true)}.
     */
    boolean whitespace() default false;

    /**
     * Line comment prefix (e.g. "//", "#", "--").
     *
     * <p>Produces skip regexp {@code escape(prefix) + ".*"} and stores the
     * original prefix as comment metadata.
     */
    String lineComment() default "";

    /**
     * Block comment delimiters as a two-element array: {start, end}.
     * For example: {"/*", "*&#47;"}.
     *
     * <p>Must contain exactly two elements. Produces skip regexp
     * {@code escape(start) + "(?:(?!" + escape(end) + ")[\\s\\S])*" + escape(end)}
     * and stores original delimiters as comment metadata.
     */
    String[] blockComment() default {};

    /**
     * Deprecated block-comment start delimiter.
     *
     * <p>Use {@link #blockComment()} instead.
     */
    @Deprecated
    String start() default "";

    /**
     * Deprecated block-comment end delimiter.
     *
     * <p>Use {@link #blockComment()} instead.
     */
    @Deprecated
    String end() default "";
}
