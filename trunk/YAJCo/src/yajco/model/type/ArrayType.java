package yajco.model.type;

import tuke.pargen.annotation.Before;

public class ArrayType extends ComponentType {
    @Before({"array", "of"})
    public ArrayType(Type componentType) {
        super(componentType);
    }
}
