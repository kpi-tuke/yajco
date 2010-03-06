package yajco.model.pattern.impl;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Optional;
import tuke.pargen.annotation.Token;
import yajco.model.Concept;
import yajco.model.Property;
import yajco.model.pattern.NotationPartPattern;

public class References implements NotationPartPattern {
    private Concept concept;

    private Property property;

    @Before({"References", "("})
    @After(")")
    public References(
            @tuke.pargen.annotation.reference.References(Concept.class) String name,
            @Optional
            @Before({",", "property", "="})
            @Token("name")
            @tuke.pargen.annotation.reference.References(value = Property.class, path = "ancestor::yajco.model.Concept//yajco.model.Property") String property) {
    }

    @Exclude
    public References(Concept concept, Property property) {
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
