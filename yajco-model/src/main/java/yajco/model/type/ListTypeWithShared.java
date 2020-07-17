package yajco.model.type;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class ListTypeWithShared extends ComponentType {
    @Before({"list", "of"})
    @After({"with", "shared", "part"})
    public ListTypeWithShared(Type componentType) {
        super(componentType);
    }

    @Exclude
    public ListTypeWithShared(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }

    //needed for XML binding
    @Exclude
    private ListTypeWithShared() {
        super(null);
    }
}
