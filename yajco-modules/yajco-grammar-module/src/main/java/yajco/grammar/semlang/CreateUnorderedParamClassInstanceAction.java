package yajco.grammar.semlang;

import java.util.List;

public class CreateUnorderedParamClassInstanceAction extends CreateInstanceAction {
    private final List<RValue> parameters;

    public CreateUnorderedParamClassInstanceAction(List<RValue> parameters) {
        this.parameters = parameters;
    }

    public List<RValue> getParameters() {
        return parameters;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CREATE_UNORDERED_PARAM_CLASS_INST;
    }
}
