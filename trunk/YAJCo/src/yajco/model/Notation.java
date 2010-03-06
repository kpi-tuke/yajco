package yajco.model;

import java.util.ArrayList;
import java.util.List;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Range;
import yajco.Utilities;

public class Notation {
    private List<NotationPart> parts;

    public Notation(@Range(minOccurs = 1) NotationPart[] parts) {
        this.parts = Utilities.asList(parts);
    }

    @Exclude
    public Notation() {
        parts = new ArrayList<NotationPart>();
    }

    public List<NotationPart> getParts() {
        return parts;
    }

    public void addPart(NotationPart part) {
        parts.add(part);
    }
}
