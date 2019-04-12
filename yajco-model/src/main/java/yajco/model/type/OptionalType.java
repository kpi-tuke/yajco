package yajco.model.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class OptionalType extends ComponentType {
    @Before({"optional"})
    public OptionalType(Type componentType) {
        super(componentType);
    }

    @Exclude
    public OptionalType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }

    //needed for XML binding
    @Exclude
    private OptionalType() {
        super(null);
    }
}
