package yajco.grammar.semlang;

import yajco.model.type.ComponentType;
import yajco.model.type.Type;

public class ConvertListWithSharedToCollectionAction extends ConvertAction {
    private final ComponentType resultCollectionType;
    private String sharedPartName;

    public ConvertListWithSharedToCollectionAction(ComponentType resultCollectionType, String sharedPartName, RValue rValue) {
        super(rValue);
        this.resultCollectionType = resultCollectionType;
        this.sharedPartName = sharedPartName;
    }

    public Type getResultCollectionInnerType() {
        return resultCollectionType.getComponentType();
    }

    public ComponentType getResultCollectionType() {
        return resultCollectionType;
    }

    public String getSharedPartName() {
        return sharedPartName;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CONVERT_LIST_WITH_SHARED_TO_COLLECTION;
    }
}
