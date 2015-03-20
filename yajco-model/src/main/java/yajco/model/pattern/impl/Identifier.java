package yajco.model.pattern.impl;

import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.PropertyPattern;

public class Identifier extends PropertyPattern {

    private String unique;

    @Before("Identifier")
    public Identifier() {
        super(null);
    }

    @Exclude
    public Identifier(Object sourceElement) {
        super(sourceElement);
    }

    public String getUnique() {
        return unique;
    }
}
