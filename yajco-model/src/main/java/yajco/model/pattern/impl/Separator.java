package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class Separator extends NotationPartPattern {

    private String value;

    @Before({"Separator", "("})
    @After(")")
    public Separator(String stringValue) {
        super(null);
        this.value = stringValue;
    }

    @Exclude
    public Separator() {
        super(null);
    }

    @Exclude
    public Separator(String value, Object sourceElement) {
        super(sourceElement);
    }

    public String getValue() {
        return value;
    }
}
