package yajco.model.pattern.impl.printer;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class NewLine extends NotationPartPattern {
    private boolean before = true;
    private boolean after = false;

    @Before("NewLine")
    public NewLine() {
        super(null);
    }

    //    @Before({"NewLine", "("})
    //    @After(")")
    //    public NewLine(String before, String after) {
    //    }

    @Exclude
    public NewLine(Object sourceElement) {
        super(sourceElement);
    }
}
