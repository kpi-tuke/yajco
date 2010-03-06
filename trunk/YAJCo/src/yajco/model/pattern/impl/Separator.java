package yajco.model.pattern.impl;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class Separator implements NotationPartPattern {
    private String value;

    @Before({"Separator", "("})
    @After(")")
    public Separator(String stringValue) {
        this.value = stringValue;
    }

    @Exclude
    public Separator() {
    }

    public String getValue() {
        return value;
    }
}
