
package yajco.model.pattern.impl.printer;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Token;
import yajco.model.pattern.NotationPartPattern;

public class Indent implements NotationPartPattern {
    int level = 1;

    @Before("Indent")
    public Indent() {
    }

    @Before({"Indent", "("})
    @After(")")
    public Indent(@Token("INT_VALUE") int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
