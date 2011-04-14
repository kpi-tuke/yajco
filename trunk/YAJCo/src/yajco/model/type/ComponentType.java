package yajco.model.type;

import yajco.annotation.Exclude;

public abstract class ComponentType extends Type {

    private final Type componentType;

    public ComponentType(Type componentType) {
        super(null);
        this.componentType = componentType;
    }

    @Exclude
    public ComponentType(Type componentType, Object sourceElement) {
        super(sourceElement);
        this.componentType = componentType;
    }

    public Type getComponentType() {
        return componentType;
    }
}
