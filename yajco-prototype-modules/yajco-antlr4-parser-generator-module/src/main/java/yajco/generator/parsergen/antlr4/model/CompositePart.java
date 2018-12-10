package yajco.generator.parsergen.antlr4.model;

import java.util.List;

public abstract class CompositePart extends Part {
    protected final List<Part> parts;

    public CompositePart(List<Part> parts) {
        this.parts = parts;

        for (Part part : this.parts) {
            part.setParent(this);
        }
    }

    public void addPart(Part part) {
        this.parts.add(part);
    }

    public List<Part> getParts() {
        return parts;
    }
}
