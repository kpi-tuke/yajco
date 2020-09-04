package yajco.grammar.semlang;

import yajco.model.type.ComponentType;
import yajco.model.type.Type;

public class ConvertListToCollectionAction extends ConvertAction {

    private final ComponentType resultCollectionType;

    public ConvertListToCollectionAction(ComponentType resultCollectionType, RValue rValue) {
        super(rValue);
        this.resultCollectionType = resultCollectionType;
    }

    public Type getResultCollectionInnerType() {
        return resultCollectionType.getComponentType();
    }

    public ComponentType getResultCollectionType() {
        return resultCollectionType;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CONVERT_LIST_TO_COLLECTION;
    }
}
