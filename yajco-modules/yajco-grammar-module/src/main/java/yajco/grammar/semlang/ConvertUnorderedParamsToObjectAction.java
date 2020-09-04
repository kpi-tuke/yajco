package yajco.grammar.semlang;

import yajco.model.type.ComponentType;
import yajco.model.type.Type;

public class ConvertUnorderedParamsToObjectAction extends ConvertAction {
    private final ComponentType resultType;

    public ConvertUnorderedParamsToObjectAction(ComponentType resultType, RValue rValue) {
        super(rValue);
        this.resultType = resultType;
    }

    public Type getResultInnerType() {
        return resultType.getComponentType();
    }

    public Type getResultType() {
        return resultType;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CONVERT_UNORDERED_PARAMS_TO_OBJECT;
    }
}
