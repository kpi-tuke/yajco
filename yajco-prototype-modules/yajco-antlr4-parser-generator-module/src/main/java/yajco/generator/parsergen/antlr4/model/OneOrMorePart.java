package yajco.generator.parsergen.antlr4.model;

public class OneOrMorePart extends Part {
    private final Part part;

    public OneOrMorePart(Part part) {
        this.part = part;
        this.part.setParent(this);
    }

    @Override
    protected String generatePart() {
        return this.part.generate() + "+";
    }
}
