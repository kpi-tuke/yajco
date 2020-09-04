package yajco.grammar.semlang;

import yajco.model.type.ComponentType;

public class AddElementToCollectionAction extends ValueAction {

    private final ComponentType componentType;

    public AddElementToCollectionAction(ComponentType componentType, LValue lValue, RValue rValue) {
        super(lValue, rValue);
        this.componentType = componentType;
    }

    public AddElementToCollectionAction(LValue lValue, RValue rValue) {
        super(lValue, rValue);
        this.componentType = null;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.ADD_ELEMENT_TO_COLLECTION;
    }
}
