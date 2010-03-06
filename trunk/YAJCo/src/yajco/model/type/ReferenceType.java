package yajco.model.type;

import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.reference.References;
import yajco.model.Concept;

public class ReferenceType implements Type {
    private Concept concept;

    public ReferenceType(@References(Concept.class) String name) {
    }

    @Exclude
    public ReferenceType(Concept concept) {
        this.concept = concept;
    }

    public Concept getConcept() {
        return concept;
    }
}
