package yajco.lexer;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private final String input;
    private final LinkedHashMap<Integer, Pattern> tokens;
    private final List<Pattern> skips;

    private int position = 0;
    private int line = 1;
    private int column = 1;

    /**
     * @param input Input sentence.
     * @param tokens Maps token type numbers to Java regex patterns.
     * @param skips Java regex patterns to be ignored when matched.
     */
    public Lexer(String input, LinkedHashMap<Integer, Pattern> tokens, List<Pattern> skips) {
        this.input = input;
        this.tokens = tokens;
        this.skips = skips;
    }

    public int getLine() {
        return this.line;
    }

    public int getColumn() {
        return this.column;
    }

    public char getCurrentCharacter() {
        return this.input.charAt(this.position);
    }

    public Token nextToken() throws LexerException {
        // Skip white spaces
        skipWhiteSpaces();

        // Return EOF at the end of input
        if (input.length() == this.position) {
            return new Token(Token.EOF, "", null, null);
        }

        // Search for the longest matching pattern - test every pattern
        return findToken();
    }

    private Token findToken() throws LexerException {
        Matcher matcher = null;
        int longest = 0;
        Token token = null;
        for (Map.Entry<Integer, Pattern> entry : this.tokens.entrySet()) {
            if (matcher == null) {
                matcher = entry.getValue().matcher(this.input);
            } else {
                matcher.usePattern(entry.getValue());
            }
            matcher.useTransparentBounds(true);
            matcher.region(this.position, this.input.length());
            if (matcher.lookingAt()) {
                String group = matcher.group();
                int start = matcher.start();
                int end = matcher.end();
                if (longest < group.length()) {
                    longest = group.length();
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        if (matcher.group(i) != null) {
                            group = matcher.group(i);
                            start = matcher.start(i);
                            end = matcher.end(i);
                            break;
                        }
                    }

                    //Create token
                    token = new Token(entry.getKey(), group,
                                new Token.Range<>(
                                    new Token.Position(start,
                                            indexToLineCol(start).getKey(),
                                            indexToLineCol(start).getValue()),
                                    new Token.Position(end - 1,
                                            indexToLineCol(end - 1).getKey(),
                                            indexToLineCol(end - 1).getValue())
                                ),
                                new Token.Range<>(
                                        new Token.Position(matcher.start(), this.line, this.column),
                                        new Token.Position(matcher.end() - 1,
                                                indexToLineCol(matcher.end() - 1).getKey(),
                                                indexToLineCol(matcher.end() - 1).getValue())
                                ));
                }
            }
        }

        //Return the longest matching token, consume it from input
        if (token != null) {
            consumeInput(longest);
            return token;
        }

        throw new LexerException("No token recognized at " + this.line + ":" + this.column);
    }

    private void skipWhiteSpaces() {
        boolean matched;
        do {
            matched = false;
            Matcher matcher = null;
            for (Pattern skip : this.skips) {
                if (matcher == null) {
                    matcher = skip.matcher(this.input);
                } else {
                    matcher.usePattern(skip);
                }
                matcher.useTransparentBounds(true);
                matcher.region(this.position, this.input.length());
                if (matcher.lookingAt()) {
                    //Consume the white space from the input
                    consumeInput(matcher.group().length());
                    matched = true;
                    matcher = null;
                    break;
                }
            }
        } while (matched);
    }

    private Map.Entry<Integer, Integer> indexToLineCol(int idx) {
        int line = 1;
        int col = 1;
        for (int i = 0; i <= idx; i++) {
            char c = this.input.charAt(i);
            col++;
            if (c == '\n') {
                col = 1;
                line++;
            }
        }
        return new AbstractMap.SimpleEntry<>(line, col);
    }

    public void consumeInput(int length) {
        for (int i = 0; i < length; i++) {
            char c = this.input.charAt(this.position+i);
            this.column++;
            if (c == '\n') {
                this.column = 1;
                this.line++;
            }
        }
        this.position += length;
    }
}
