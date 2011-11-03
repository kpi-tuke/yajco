package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Optional;
import yajco.annotation.Token;
import yajco.model.Concept;
import yajco.model.Property;
import yajco.model.pattern.NotationPartPattern;

public class References extends NotationPartPattern {

    private Concept concept;
    private Property property;

    @Before({"References", "("})
    @After(")")
    public References(
            @yajco.annotation.reference.References(Concept.class) String name,
            @Optional
            @Before({",", "property", "="})
            @Token("name")
            @yajco.annotation.reference.References(value = Property.class, path = "ancestor::yajco.model.Concept//yajco.model.Property") String property) {
        super(null);
    }

    @Before({"References", "("})
    @After(")")
    public References(
            @yajco.annotation.reference.References(Concept.class) String name) {
        super(null);
    }

    @Exclude
    public References(Concept concept, Property property, Object sourceElement) {
        super(sourceElement);
        this.concept = concept;
        this.property = property;
    }

    public Concept getConcept() {
        return concept;
    }

    public Property getProperty() {
        return property;
    }
}
