package yajco.grammar.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.type.ComponentType;
import yajco.model.type.Type;

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
