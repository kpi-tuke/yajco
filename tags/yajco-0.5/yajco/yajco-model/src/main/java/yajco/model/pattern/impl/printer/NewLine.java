package yajco.model.pattern.impl.printer;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class NewLine extends NotationPartPattern {
//    private yajco.annotation.printer.NewLine.Position position = yajco.annotation.printer.NewLine.Position.AFTER;

    @Before("NewLine")
    public NewLine() {
        super(null);
    }

//    @Before({"NewLine", "("})
//    @After(")")
//    public NewLine(yajco.annotation.printer.NewLine.Position position) {
//         super(null);
//         this.position = position;
//    }

    @Exclude
    public NewLine(Object sourceElement) {
        super(sourceElement);
    }

//    /**
//     * @return the position
//     */
//    public yajco.annotation.printer.NewLine.Position getPosition() {
//        return position;
//    }
//
//    /**
//     * @param position the position to set
//     */
//    public void setPosition(yajco.annotation.printer.NewLine.Position position) {
//        this.position = position;
//    }
}
