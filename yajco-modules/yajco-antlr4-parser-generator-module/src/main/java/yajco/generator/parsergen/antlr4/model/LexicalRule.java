package yajco.generator.parsergen.antlr4.model;

public class LexicalRule implements Element {
    private final String name;
    private final String body;
    private final boolean skip;

    public LexicalRule(String name, String body) {
        this(name, body, false);
    }

    public LexicalRule(String name, String body, boolean skip) {
        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0)))
            throw new IllegalArgumentException("Lexical rules must begin with a capital letter.");
        this.name = name;
        this.body = body;
        this.skip = skip;
    }

    public String generate() {
        return this.name + " : " + this.body + ((skip) ? " -> skip" : "") + ";";
    }
}
