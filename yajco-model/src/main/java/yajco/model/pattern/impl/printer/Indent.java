package yajco.model.pattern.impl.printer;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Token;
import yajco.model.pattern.NotationPartPattern;

public class Indent extends NotationPartPattern {
    int level = 1;

    @Before("Indent")
    public Indent() {
        super(null);
    }
    
    @Before({"Indent", "("})
    @After(")")
    public Indent(@Token("INT_VALUE") int level) {
        super(null);
        this.level = level;
    }

    @Exclude
    public Indent(Object sourceElement) {
        super(sourceElement);
    }

    public int getLevel() {
        return level;
    }
}
