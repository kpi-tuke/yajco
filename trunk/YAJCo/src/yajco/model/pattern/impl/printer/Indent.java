
package yajco.model.pattern.impl.printer;

import tuke.pargen.annotation.Before;
import yajco.model.pattern.NotationPartPattern;

public class Indent implements NotationPartPattern {
    @Before("Indent")
    public Indent() {
    }
}
