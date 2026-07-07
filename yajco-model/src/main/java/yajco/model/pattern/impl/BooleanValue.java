package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class BooleanValue extends NotationPartPattern {
    private String trueToken = "true";
    private String falseToken = "false";

    @Before({"BooleanValue", "("})
    @After(")")
    public BooleanValue(
            @yajco.annotation.Token("STRING_VALUE") String trueToken,
            @Before(",") @yajco.annotation.Token("STRING_VALUE") String falseToken) {
        super(null);
        this.trueToken = trueToken;
        this.falseToken = falseToken;
    }

    @Exclude
    public BooleanValue() {
        super(null);
    }

    @Exclude
    public BooleanValue(String trueToken, String falseToken, Object sourceElement) {
        super(sourceElement);
        this.trueToken = trueToken;
        this.falseToken = falseToken;
    }

    public String getTrueToken() {
        return trueToken;
    }

    public String getFalseToken() {
        return falseToken;
    }
}
