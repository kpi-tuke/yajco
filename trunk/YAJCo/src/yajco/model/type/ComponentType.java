package yajco.model.type;

public abstract class ComponentType implements Type {
    private final Type componentType;

    public ComponentType(Type componentType) {
        this.componentType = componentType;
    }

    public Type getComponentType() {
        return componentType;
    }
}
