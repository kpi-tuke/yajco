package yajco.generator.parsergen.antlr4.model;

public class ZeroOrMorePart extends Part {
    private final Part part;

    public ZeroOrMorePart(Part part) {
        this.part = part;
        this.part.setParent(this);
    }

    @Override
    protected String generatePart() {
        return this.part.generate() + "*";
    }
}
