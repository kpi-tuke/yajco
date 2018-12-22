package yajco.generator.parsergen.antlr4.model;

/* Terminal or non-terminal symbol used in the right-hand side of the production rule. */
public class RulePart extends Part {
    private final String name;

    public RulePart(String name) {
        this(name, null);
    }

    public RulePart(String name, String label) {
        this.name = name;
    }
    @Override
    protected String generatePart() {
        return this.name;
    }

    public String getName() {
        return name;
    }

    public boolean isTerminal() {
        return this.name.equals(this.name.toUpperCase());
    }
}
