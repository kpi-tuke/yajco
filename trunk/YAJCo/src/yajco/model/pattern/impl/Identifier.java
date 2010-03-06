package yajco.model.pattern.impl;

import tuke.pargen.annotation.Before;
import yajco.model.pattern.PropertyPattern;

public class Identifier implements PropertyPattern {
    private String unique;

    @Before("Identifier")
    public Identifier() {
    }

    public String getUnique() {
        return unique;
    }
}
