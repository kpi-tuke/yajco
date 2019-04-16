package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class Shared extends NotationPartPattern {
    private String value;
    private String separator;

    @Before({"Shared", "part", "("})
    @After(")")
    public Shared(String sharedPart, String separator) {
        super(null);
        this.value = sharedPart;
        this.separator = separator;
    }

    @Exclude
    public Shared() {
        super(null);
    }

    @Exclude
    public Shared(String sharedPart, String separator, Object sourceElement) {
        super(sourceElement);
    }

    public String getValue() {
        return this.value;
    }

    public String getSeparator() {
        return this.separator;
    }
}
