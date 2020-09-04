package yajco.grammar.semlang;

import java.util.List;

public class CreateUnorderedParamClassInstanceAction extends CreateInstanceAction {
    private final List<RValue> parameters;
    private final String varName;

    public CreateUnorderedParamClassInstanceAction(List<RValue> parameters, String varName) {
        this.parameters = parameters;
        this.varName = varName;
    }

    public List<RValue> getParameters() {
        return parameters;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CREATE_UNORDERED_PARAM_CLASS_INST;
    }

    public String getVarName() {
        return varName;
    }
}
