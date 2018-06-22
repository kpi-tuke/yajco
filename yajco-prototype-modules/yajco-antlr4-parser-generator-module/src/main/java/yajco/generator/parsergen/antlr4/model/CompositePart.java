package yajco.generator.parsergen.antlr4.model;

import java.util.List;

public abstract class CompositePart extends Part {
    protected final List<Part> parts;

    public CompositePart(List<Part> parts) {
        if (parts.isEmpty()) throw new IllegalArgumentException();
        this.parts = parts;

        for (Part part : this.parts) {
            part.setParent(this);
        }
    }
}
