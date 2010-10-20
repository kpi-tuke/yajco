package yajco.parser.javacc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserTokenManager implements TokenManager, ParserConstants {
    private String input;

    private int line = 1;

    private int column = 1;

    private Token lastToken;

    private static final Map<Integer, Pattern> tokens = new LinkedHashMap<Integer, Pattern>();

    private static final List<Pattern> skips = new ArrayList<Pattern>();

    static {
        tokens.put(TOKENS, Pattern.compile("tokens"));
        tokens.put(_61, Pattern.compile("[=]"));
        tokens.put(SKIPS, Pattern.compile("skips"));
        tokens.put(CONCEPT, Pattern.compile("concept"));
        tokens.put(_58, Pattern.compile("[:]"));
        tokens.put(_123, Pattern.compile("[{]"));
        tokens.put(PARENTHESES, Pattern.compile("Parentheses"));
        tokens.put(_40, Pattern.compile("[(]"));
        tokens.put(_44, Pattern.compile("[,]"));
        tokens.put(_41, Pattern.compile("[)]"));
        tokens.put(OPERATOR, Pattern.compile("Operator"));
        tokens.put(PRIORITY, Pattern.compile("priority"));
        tokens.put(ASSOCIATIVITY, Pattern.compile("associativity"));
        tokens.put(LEFT, Pattern.compile("LEFT"));
        tokens.put(RIGHT, Pattern.compile("RIGHT"));
        tokens.put(NONE, Pattern.compile("NONE"));
        tokens.put(AUTO, Pattern.compile("AUTO"));
        tokens.put(ENUM, Pattern.compile("Enum"));
        tokens.put(_125, Pattern.compile("[}]"));
        tokens.put(AS, Pattern.compile("AS"));
        tokens.put(ARRAY, Pattern.compile("array"));
        tokens.put(OF, Pattern.compile("of"));
        tokens.put(LIST, Pattern.compile("list"));
        tokens.put(SET, Pattern.compile("set"));
        tokens.put(IDENTIFIER, Pattern.compile("Identifier"));
        tokens.put(CS, Pattern.compile("CS"));
        tokens.put(_124, Pattern.compile("[|]"));
        tokens.put(SEPARATOR, Pattern.compile("Separator"));
        tokens.put(RANGE, Pattern.compile("Range"));
        tokens.put(_46_46, Pattern.compile("[.][.]"));
        tokens.put(_42, Pattern.compile("[*]"));
        tokens.put(NEWLINE, Pattern.compile("NewLine"));
        tokens.put(REFERENCES, Pattern.compile("References"));
        tokens.put(PROPERTY, Pattern.compile("property"));
        tokens.put(INDENT, Pattern.compile("Indent"));
        tokens.put(FACTORY, Pattern.compile("Factory"));
        tokens.put(METHOD, Pattern.compile("method"));
        tokens.put(LANGUAGE, Pattern.compile("language"));

        tokens.put(BOOLEAN, Pattern.compile("boolean"));
        tokens.put(INTEGER, Pattern.compile("int"));
        tokens.put(REAL, Pattern.compile("real"));
        tokens.put(STRING, Pattern.compile("string"));
        tokens.put(NAME, Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)|\\[([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\]"));
        tokens.put(BOOLEAN_VALUE, Pattern.compile("true|false"));
        tokens.put(REAL_VALUE, Pattern.compile("[-]?[0-9]+[.][0-9]+((e|E)[0-9]+)?"));
        tokens.put(STRING_VALUE, Pattern.compile("\"([^\"]*)\""));
        tokens.put(INT_VALUE, Pattern.compile("[0-9]+"));

        skips.add(Pattern.compile(" "));
        skips.add(Pattern.compile("\\t"));
        skips.add(Pattern.compile("\\n"));
        skips.add(Pattern.compile("\\r"));
        skips.add(Pattern.compile("//.*"));
    }

    public ParserTokenManager(String input) {
        this.input = input;
    }

    public Token getNextToken() {
        //Skip white spaces
        skipWhiteSpaces();

        //Return EOF at the end of input
        if (input.length() == 0) {
            return Token.newToken(EOF);
        }

        //Search for the longest matching pattern - test every pattern
        return findToken();
    }

    private void skipWhiteSpaces() {
        boolean matched;
        do {
            matched = false;
            Matcher matcher = null;
            for (Pattern skip : skips) {
                if (matcher == null) {
                    matcher = skip.matcher(input);
                } else {
                    matcher.usePattern(skip);
                }
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

    private Token findToken() {
        Matcher matcher = null;
        int longest = 0;
        Token token = null;
        for (Map.Entry<Integer, Pattern> entry : tokens.entrySet()) {
            if (matcher == null) {
                matcher = entry.getValue().matcher(input);
            } else {
                matcher.usePattern(entry.getValue());
            }
            if (matcher.lookingAt()) {
                String group = matcher.group();
                if (longest < group.length()) {
                    longest = group.length();
                    int kind = entry.getKey();

                    // try to get first not null group
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        if (matcher.group(i) != null) {
                            group = matcher.group(i);
                            break;
                        }
                    }

                    //Create token                    
                    token = Token.newToken(kind, group);
                }
            }
        }

        //System.out.printf("Token %s\n", token.image);

        //Return the longest matching token, consume it from input
        if (token != null) {
            //System.out.printf("Token recognized: %s (%d) with image:'%s'\n", tokenImage[token.kind], token.kind, token.image);
            //Set the loaction info into the token and consume the token from the input
            token.beginLine = line;
            token.beginColumn = column;
            consumeInput(longest);
            token.endLine = line;
            token.endColumn = column;
            lastToken = token;
            return token;
        }

        throw new TokenMgrError(false, 0, line, column, lastToken == null ? "" : lastToken.image, input.charAt(0), TokenMgrError.LEXICAL_ERROR);
    }

    private void consumeInput(int length) {
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            column++;
            if (c == '\n') {
                column = 1;
                line++;
            }
        }
        input = input.substring(length);
    }
}
