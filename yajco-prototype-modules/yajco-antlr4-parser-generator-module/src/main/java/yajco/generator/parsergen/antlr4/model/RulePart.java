package yajco.generator.parsergen.antlr4.model;

/* Terminal or non-terminal symbol used in the right-hand side of the production rule. */
public class RulePart extends Part {
    private final String name;
    private final String label;

    public RulePart(String name) {
        this(name, null);
    }

    public RulePart(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public boolean shouldGenerateLabel() {
        return (this.label != null && !this.label.equals(this.name));
    }

    @Override
    protected String generatePart() {
        return (shouldGenerateLabel() ? this.label + "=" : "") + this.name;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return this.name.equals(this.name.toUpperCase());
    }
}
