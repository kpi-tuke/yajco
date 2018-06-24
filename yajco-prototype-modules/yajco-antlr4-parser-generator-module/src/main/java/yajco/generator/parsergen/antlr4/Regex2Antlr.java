package yajco.generator.parsergen.antlr4;

/*
Converts Java/YAJCo regexes into ANTLR4 lexical rules.

For example: aa[abc]+(bb|cc) is converted into 'aa'[abc]+('bb'|'cc').

FIXME: This converter is incomplete and will only work on basic regexes.
TODO: It will be later removed when we switch to a custom lexer.
*/
public class Regex2Antlr {
    static class ConvertException extends Exception {
        ConvertException(String message) {
            super(message);
        }
    }

    private int pos;
    private final String regex;
    private static int EOF = -1;

    public Regex2Antlr(String regex) {
        this.regex = regex;
        this.pos = -1;
    }

    private int peek() {
        if (this.pos + 1 >= regex.length()) {
            return EOF;
        }
        return this.regex.charAt(this.pos + 1);
    }

    private int consume() throws ConvertException {
        this.pos++;
        if (this.pos >= regex.length())
            throw new ConvertException("Reached EOF unexpectedly");
        return this.regex.charAt(this.pos);
    }

    private boolean isSpecialChar(int ch) {
        return "[]()*+.?|".indexOf((char) ch) != -1;
    }

    private boolean isPartOfLiteral(int ch) {
        return !isSpecialChar(ch) || ch == '\\';
    }

    public String convert() throws ConvertException {
        StringBuilder ret = new StringBuilder();
        int ch;
        while ((ch = peek()) != EOF) {
            if (isPartOfLiteral(ch)) { // literals must be enclosed in ''
                ret.append('\'');
                do {
                    ch = consume();
                    if (ch == '\\') { // escape sequence
                        // FIXME: Sequences with multiple characters are currently not considered.
                        ch = consume();
                        if (!isSpecialChar(ch)) {
                            ret.append('\\');
                        }
                        ret.append((char) ch);
                    } else {
                        ret.append((char) ch);
                    }
                } while (peek() != EOF && isPartOfLiteral(peek()));
                ret.append('\'');
            } else {
                ch = consume();
                ret.append((char) ch);
                if (ch == '[') { // character range
                    int startOfCharacterRange = ret.length() - 1;
                    int i = 0;
                    while (true) {
                        ch = consume();
                        if (ch == ']') {
                            ret.append((char) ch);
                            break;
                        } else if (ch == '\\') {
                            // Ignore escape sequences within character ranges.
                            // This is mainly so we don't end on \]
                            ret.append((char) ch);
                            ret.append((char) consume());
                        } else if (ch == '^' && i == 0) {
                            // ANTLR notates negation with a tilde before character range
                            ret.insert(startOfCharacterRange, '~');
                        } else {
                            ret.append((char) ch);
                        }
                        i++;
                    }
                }
            }
        }
        return ret.toString();
    }
}
