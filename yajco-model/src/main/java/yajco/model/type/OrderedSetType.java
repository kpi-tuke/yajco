package yajco.model.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class OrderedSetType extends ComponentType {
    @Before({"ordered", "set", "of"})
    public OrderedSetType(Type componentType) {
        super(componentType);
    }

    @Exclude
    public OrderedSetType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }

    //needed for XML binding
    @Exclude
    private OrderedSetType() {
        super(null);
    }
}
