package yajco.model.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class ListType extends ComponentType {

    @Before({"list", "of"})
    public ListType(Type componentType) {
        super(componentType);
    }

    @Exclude
    public ListType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }
}
