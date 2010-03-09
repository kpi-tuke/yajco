package yajco.model.pattern.impl;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import yajco.model.pattern.ConceptPattern;

//TODO: po zjednotenie nechat len jeden enum
public class Operator implements ConceptPattern {
    private int priority;

    private Associativity associativity;

    @Before({"Operator", "("})
    @After(")")
    public Operator(@Before({"priority", "="}) int intValue) {
        this.priority = intValue;
    }

    @Before({"Operator", "("})
    @After(")")
    public Operator(
            @Before({"priority", "="}) int intValue,
            @Before({"associativity", "="}) Associativity associativity) {
        this.priority = intValue;
        this.associativity = associativity;
    }

    @Exclude
    public Operator() {
    }

    public int getPriority() {
        return priority;
    }

    public Associativity getAssociativity() {
        return associativity;
    }
}
