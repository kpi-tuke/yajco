package yajco.generator.parsergen.antlr4.model;

/**
 * @brief Utility functions for formatting
 */
public class Formatting {
    /* Create indentation string of the given level. */
    public static String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++)
            sb.append("  ");
        return sb.toString();
    }

    /* Indent every line of the given string with the given indentation level. */
    public static String indent(String str, int level) {
        return str.replaceAll("(?m)^", indent(level));
    }
}
