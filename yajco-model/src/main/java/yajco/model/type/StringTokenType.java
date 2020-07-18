package yajco.model.type;

import yajco.annotation.Exclude;

public class StringTokenType extends Type {
    public StringTokenType() {
        super(null);
    }

    @Exclude
    public StringTokenType(Object sourceElement) {
        super(sourceElement);
    }
}
