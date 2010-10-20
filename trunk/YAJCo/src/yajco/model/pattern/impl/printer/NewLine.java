
package yajco.model.pattern.impl.printer;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import yajco.model.pattern.NotationPartPattern;

public class NewLine implements NotationPartPattern {
    private boolean before = true;
    private boolean after = false;

    @Before("NewLine")
    public NewLine() {
    }

//    @Before({"NewLine", "("})
//    @After(")")
//    public NewLine(String before, String after) {
//    }


}
