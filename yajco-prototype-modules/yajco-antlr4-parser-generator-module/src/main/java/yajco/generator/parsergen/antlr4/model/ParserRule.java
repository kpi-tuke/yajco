package yajco.generator.parsergen.antlr4.model;

public class ParserRule implements Element {
    private final String name;
    private final String returns;
    private final Part body; // FIXME: This should be a model element, not a string.

    public ParserRule(String name, String returns, Part body) {
        if (name.isEmpty() || Character.isUpperCase(name.charAt(0)))
            throw new IllegalArgumentException("Parser rules must not begin with a capital letter.");
        this.name = name;
        this.returns = returns;
        this.body = body;
    }

    public String generate() {
        return this.name +
                (this.returns.isEmpty() ? "" : " returns [" + this.returns + "]") +
                " :\n" +
                Formatting.indent(this.body.generate(), 1) + ";";
    }
}
