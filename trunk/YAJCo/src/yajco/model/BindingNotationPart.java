package yajco.model;

import yajco.model.pattern.PatternSupport;
import java.util.List;
import yajco.model.pattern.NotationPartPattern;

public abstract class BindingNotationPart extends PatternSupport<NotationPartPattern> implements NotationPart {

    public BindingNotationPart(Object sourceElement) {
        super(sourceElement);
    }

    public BindingNotationPart(List<NotationPartPattern> patterns, Object sourceElement) {
        super(patterns, sourceElement);
    }
}
