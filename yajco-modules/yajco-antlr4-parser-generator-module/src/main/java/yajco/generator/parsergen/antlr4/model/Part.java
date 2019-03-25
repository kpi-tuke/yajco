package yajco.generator.parsergen.antlr4.model;

/* Part of the right-hand side of the production rule. */
public abstract class Part implements Element {
    public enum Associativity {
        Unspecified,
        Left,
        Right
    }

    protected String codeBefore = "";
    protected String codeAfter = "";
    protected Associativity associativity = Associativity.Unspecified;
    private String label = "";
    protected Part parent = null;

    public void setCodeBefore(String codeBefore) {
        this.codeBefore = codeBefore;
    }

    public void setCodeAfter(String codeAfter) {
        this.codeAfter = codeAfter;
    }

    public void setAssociativity(Associativity associativity) {
        this.associativity = associativity;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    void setParent(Part parent) {
        this.parent = parent;
    }

    public String getCodeBefore() {
        return codeBefore;
    }

    public String getCodeAfter() {
        return codeAfter;
    }

    public Associativity getAssociativity() {
        return associativity;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder();
        if (!this.codeBefore.isEmpty()) {
            sb.append("{\n").append(Formatting.indent(this.codeBefore, 1)).append("\n}\n");
        }
        switch (this.associativity) {
            case Left:
                sb.append("<assoc=left>\n");
                break;
            case Right:
                sb.append("<assoc=right>\n");
                break;
            default:
                break;
        }
        if (!this.label.isEmpty()) {
            sb.append(this.label).append("=");
        }
        sb.append(generatePart());
        if (!this.codeAfter.isEmpty()) {
            sb.append("\n{\n").append(Formatting.indent(this.codeAfter, 1)).append("\n}\n");
        }

        return sb.toString();
    }

    abstract protected String generatePart();
}
