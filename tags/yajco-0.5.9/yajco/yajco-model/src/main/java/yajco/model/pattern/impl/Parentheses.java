package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Token;
import yajco.model.pattern.ConceptPattern;

public class Parentheses extends ConceptPattern {

    private String left = "(";
    private String right = ")";

    @Before("Parentheses")
    public Parentheses() {
        super(null);
    }

    @Before({"Parentheses", "("})
    @After(")")
    public Parentheses(@Token("STRING_VALUE") String left, @Before(",") @Token("STRING_VALUE") String right) {
        super(null);
        this.left = left;
        this.right = right;
    }

    @Exclude
    public Parentheses(Object sourceElement) {
        super(sourceElement);
    }

    @Exclude
    public Parentheses(String left, String right, Object sourceElement) {
        super(sourceElement);
        this.left = left;
        this.right = right;
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }
}
