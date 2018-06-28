package yajco.generator.parsergen.antlr4.model;

import java.util.List;

/**
 * Represents ANTLR4 grammar specification along with embedded Java actions.
 */
public class Grammar implements Element {
    private final String name;
    private final String header;
    private final List<String> implicitTokens;
    private final List<ParserRule> parserRules;
    private final List<LexicalRule> lexicalRules;

    public Grammar(String name, String header, List<String> implicitTokens, List<ParserRule> parserRules, List<LexicalRule> lexicalRules) {
        this.name = name;
        this.header = header;
        this.implicitTokens = implicitTokens;
        this.parserRules = parserRules;
        this.lexicalRules = lexicalRules;
    }

    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append("grammar " + this.name + ";\n\n");
        if (this.header != null && !this.header.isEmpty()) {
            sb.append("@header {\n").append(this.header).append("\n}\n\n");
        }

        if (this.implicitTokens != null && !this.implicitTokens.isEmpty()) {
            sb.append("tokens {\n");
            sb.append(Formatting.indent(generateImplicitTokens(), 1));
            sb.append("}\n\n");
        }

        sb.append("// Parser rules:\n")
          .append(generateParserRules() + "\n")
          .append("// Lexer rules:\n")
          .append(generateLexicalRules());

        return sb.toString();
    }

    private String generateImplicitTokens() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.implicitTokens.size(); i++) {
            String implicitToken = this.implicitTokens.get(i);
            sb.append(implicitToken);
            if (i != this.implicitTokens.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        return sb.toString();
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
