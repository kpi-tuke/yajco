package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class Shared extends NotationPartPattern {
    private String separator;

    @Before({"Shared", "part", "("})
    @After(")")
    public Shared(String separator) {
        super(null);
        this.separator = separator;
    }

    @Exclude
    public Shared() {
        super(null);
    }

    @Exclude
    public Shared(String separator, Object sourceElement) {
        super(sourceElement);
        this.separator = separator;
    }

    public String getSeparator() {
        return this.separator;
    }
}
