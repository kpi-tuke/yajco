package yajco.grammar.semlang;

public class CreateOptionalClassInstanceAction extends CreateInstanceAction {

    private final RValue parameter;

    public CreateOptionalClassInstanceAction(RValue parameter) {
        this.parameter = parameter;
    }

    public RValue getParameter() {
        return parameter;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CREATE_OPTIONAL_CLASS_INST;
    }
}
