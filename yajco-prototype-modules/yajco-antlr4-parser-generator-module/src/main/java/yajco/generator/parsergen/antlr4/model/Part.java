package yajco.generator.parsergen.antlr4.model;

/* Part of the right-hand side of the production rule. */
public abstract class Part implements Element {
    protected String codeBefore = "";
    protected String codeAfter = "";
    protected Part parent = null;

    public void setCodeBefore(String codeBefore) {
        this.codeBefore = codeBefore;
    }

    public void setCodeAfter(String codeAfter) {
        this.codeAfter = codeAfter;
    }

    void setParent(Part parent) {
        this.parent = parent;
    }

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder();
        if (!this.codeBefore.isEmpty()) {
            sb.append("{\n").append(Formatting.indent(this.codeBefore, 1)).append("\n}\n");
        }
        sb.append(generatePart());
        if (!this.codeAfter.isEmpty()) {
            sb.append("\n{\n").append(Formatting.indent(this.codeAfter, 1)).append("\n}\n");
        }

        return sb.toString();
    }

    abstract protected String generatePart();
}
