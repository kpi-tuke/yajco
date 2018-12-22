package yajco.generator.parsergen.antlr4.model;

import java.util.Collections;
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
        part.setParent(this);
    }

    public void setPart(int i, Part part) {
        this.parts.set(i, part);
        part.setParent(this);
    }

    public List<Part> getParts() {
        return Collections.unmodifiableList(this.parts);
    }
}
