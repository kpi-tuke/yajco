package yajco.model.pattern.impl;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import yajco.model.pattern.ConceptPattern;

public class Parentheses implements ConceptPattern {
    private String left = "(";

    private String right = ")";

    @Before("Parentheses")
    public Parentheses() {
    }

    @Before({"Parentheses", "("})
    @After(")")
    public Parentheses(String left, @Before(",") String right) {
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
