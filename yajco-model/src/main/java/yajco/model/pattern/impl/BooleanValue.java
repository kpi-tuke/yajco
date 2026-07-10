package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

import java.util.Arrays;

public class BooleanValue extends NotationPartPattern {
    private String[] trueToken = {"true"};
    private String[] falseToken = {"false"};

    @Before({"BooleanValue", "("})
    @After(")")
    public BooleanValue(
            @yajco.annotation.Token("STRING_VALUE") String trueToken,
            @Before(",") @yajco.annotation.Token("STRING_VALUE") String falseToken) {
        this(new String[] {trueToken}, new String[] {falseToken}, null);
    }

    @Before({"BooleanValue", "("})
    @After(")")
    public BooleanValue(
            @Before("{") @After("}") @yajco.annotation.Separator(",") @yajco.annotation.Token("STRING_VALUE") String[] trueTokens,
            @Before({",", "{"}) @After("}") @yajco.annotation.Separator(",") @yajco.annotation.Token("STRING_VALUE") String[] falseTokens) {
        this(trueTokens, falseTokens, null);
    }

    @Exclude
    public BooleanValue() {
        super(null);
    }

    @Exclude
    public BooleanValue(String trueToken, String falseToken, Object sourceElement) {
        this(new String[] {trueToken}, new String[] {falseToken}, sourceElement);
    }

    @Exclude
    public BooleanValue(String[] trueToken, String[] falseToken, Object sourceElement) {
        super(sourceElement);
        this.trueToken = copyOf(trueToken);
        this.falseToken = copyOf(falseToken);
    }

    public String getTrueToken() {
        return firstToken(trueToken);
    }

    public String getFalseToken() {
        return firstToken(falseToken);
    }

    public String[] getTrueTokens() {
        return copyOf(trueToken);
    }

    public String[] getFalseTokens() {
        return copyOf(falseToken);
    }

    private static String firstToken(String[] tokens) {
        return tokens.length == 0 ? "" : tokens[0];
    }

    private static String[] copyOf(String[] tokens) {
        return tokens == null ? new String[0] : Arrays.copyOf(tokens, tokens.length);
    }
}
