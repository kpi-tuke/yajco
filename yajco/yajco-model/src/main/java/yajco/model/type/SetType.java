package yajco.model.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class SetType extends ComponentType {

    @Before({"set", "of"})
    public SetType(Type componentType) {
        super(componentType);
    }

    @Exclude
    public SetType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }
}
