package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class Token extends NotationPartPattern {

    private String name;

    @Before({"Token", "("})
    @After(")")
    public Token(@yajco.annotation.Token("STRING_VALUE") String name) {
        super(null);
        this.name = name;
    }

    @Exclude
    public Token(String name, Object sourceElement) {
        super(sourceElement);
        this.name = name;
    }

    //needed for XML binding
    @Exclude
    private Token() {
        super(null);
    }

    public String getName() {
        return name;
    }
}
