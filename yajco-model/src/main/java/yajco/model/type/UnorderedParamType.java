package yajco.model.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class UnorderedParamType extends ComponentType {
    @Before({"unordered", "param"})
    public UnorderedParamType(Type componentType) {
        super(componentType);
    }

    @Exclude
    public UnorderedParamType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }

    //needed for XML binding
    @Exclude
    private UnorderedParamType() {
        super(null);
    }
}
