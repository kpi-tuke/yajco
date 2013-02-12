package yajco.model.pattern.impl;

import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.ConceptPattern;

public class Enum extends ConceptPattern {

    @Before("Enum")
    public Enum() {
        super(null);
    }

    @Exclude
    public Enum(Object sourceElement) {
        super(sourceElement);
    }
}
