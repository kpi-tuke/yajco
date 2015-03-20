package yajco.model.type;

import yajco.annotation.Exclude;
import yajco.annotation.reference.References;
import yajco.model.Concept;

public class ReferenceType extends Type {

    private Concept concept;

    public ReferenceType(@References(Concept.class) String name) {
        super(null);
    }

    @Exclude
    public ReferenceType(Concept concept, Object sourceElement) {
        super(sourceElement);
        this.concept = concept;
    }
    
    //needed for XML binding
    @Exclude
    private ReferenceType() {
        super(null);
    }

    public Concept getConcept() {
        return concept;
    }
}
