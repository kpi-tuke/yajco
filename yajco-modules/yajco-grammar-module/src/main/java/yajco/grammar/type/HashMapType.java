package yajco.grammar.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.type.ComponentType;
import yajco.model.type.Type;

public class HashMapType extends ComponentType {
    @Before({"hash", "map", "of"})
    public HashMapType(Type componentType) {
        super(componentType);
    }

    @Exclude
    public HashMapType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }

    //needed for XML binding
    @Exclude
    private HashMapType() {
        super(null);
    }
}
