package yajco.model.pattern.impl;

import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class UniqueValues extends NotationPartPattern {
    @Before({"Unique", "values"})
    public UniqueValues() {
        super(null);
    }

    @Exclude
    public UniqueValues(Object sourceElement) {
        super(sourceElement);
    }
}
