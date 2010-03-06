package yajco.model;

import yajco.model.pattern.PatternSupport;
import java.util.List;
import yajco.model.pattern.NotationPartPattern;

public abstract class BindingNotationPart extends PatternSupport<NotationPartPattern> implements NotationPart {
    public BindingNotationPart() {
    }

    public BindingNotationPart(List<NotationPartPattern> patterns) {
        super(patterns);
    }
}
