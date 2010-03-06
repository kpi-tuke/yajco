package yajco.model.pattern.impl;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Token;
import yajco.model.pattern.NotationPartPattern;

public class Range implements NotationPartPattern {
    public static final int INFINITY = -1;

    private int minOccurs = 0;

    private int maxOccurs = INFINITY;

    @Before({"Range", "("})
    @After({"..", "*", ")"})
    public Range(@Token("INT_VALUE") int minOccurs) {
        this.minOccurs = minOccurs;
    }

    @Before({"Range", "("})
    @After(")")
    public Range(@Token("INT_VALUE") int minOccurs, @Before("..") @Token("INT_VALUE") int maxOccurs) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
    }

    //TODO: Ak bolo toto odkomentovane parser generator negeneroval dobre vystup
//    @Before({"Range", "(", "*", ")"})
//    public Range() {
//    }
    @Exclude
    public Range() {
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }
}
