package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

/**
 * Pattern representing a boolean flag in the grammar.
 * The token is optional - its presence means true, its absence means false.
 */
public class Flag extends NotationPartPattern {

    private String token;

    @Before({"Flag", "("})
    @After(")")
    public Flag(@yajco.annotation.Token("STRING_VALUE") String token) {
        super(null);
        this.token = token;
    }

    @Exclude
    public Flag(String token, Object sourceElement) {
        super(sourceElement);
        this.token = token;
    }

    //needed for XML binding
    @Exclude
    private Flag() {
        super(null);
    }

    public String getToken() {
        return token;
    }
}
