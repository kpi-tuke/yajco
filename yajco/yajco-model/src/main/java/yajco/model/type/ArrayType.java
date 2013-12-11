package yajco.model.type;

import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class ArrayType extends ComponentType {

    @Before({"array", "of"})
    public ArrayType(Type componentType) {
        super(componentType);
    }

    @Exclude
    public ArrayType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }
    
    //needed for XML binding
    @Exclude
    private ArrayType() {
        super(null);
    }
}
