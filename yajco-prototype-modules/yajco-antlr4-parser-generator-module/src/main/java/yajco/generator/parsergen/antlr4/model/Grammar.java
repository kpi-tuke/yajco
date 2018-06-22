package yajco.generator.parsergen.antlr4.model;

import java.util.List;

/**
 * Represents ANTLR4 grammar specification along with embedded Java actions.
 */
public class Grammar implements Element {
    private final String name;
    private final String header;
    private final List<ParserRule> parserRules;
    private final List<LexicalRule> lexicalRules;

    public Grammar(String name, String header, List<ParserRule> parserRules, List<LexicalRule> lexicalRules) {
        this.name = name;
        this.header = header;
        this.parserRules = parserRules;
        this.lexicalRules = lexicalRules;
    }

    public String generate() {
        return
                "grammar " + this.name + ";\n\n" +
                "@header {\n" +
                this.header +
                "\n}\n\n" +
                "// Parser rules:\n" +
                generateParserRules() + "\n" +
                "// Lexical rules:\n" +
                generateLexicalRules();
    }

    private String generateParserRules() {
        StringBuilder s = new StringBuilder();

        for (ParserRule rule : this.parserRules) {
            s.append(rule.generate()).append("\n");
        }

        return s.toString();
    }

    private String generateLexicalRules() {
        StringBuilder s = new StringBuilder();

        for (LexicalRule rule : this.lexicalRules) {
            s.append(rule.generate()).append("\n");
        }

        return s.toString();
    }
}
