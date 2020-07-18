package yajco.grammar.semlang;

public class CreateSymbolStringTokenClassInstanceAction extends CreateInstanceAction {
    private final RValue parameter;

    public CreateSymbolStringTokenClassInstanceAction(RValue parameter) {
        this.parameter = parameter;
    }

    public RValue getParameter() {
        return parameter;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CREATE_SYMBOL_STRING_TOKEN_CLASS_INST;
    }
}
