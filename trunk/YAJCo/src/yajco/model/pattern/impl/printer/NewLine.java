
package yajco.model.pattern.impl.printer;

import tuke.pargen.annotation.Before;
import yajco.model.pattern.NotationPartPattern;

public class NewLine implements NotationPartPattern {
    @Before("NewLine")
    public NewLine() {
    }
}
