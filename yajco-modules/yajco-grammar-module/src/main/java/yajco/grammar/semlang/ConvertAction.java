package yajco.grammar.semlang;

public abstract class ConvertAction extends RValueAction {

    public ConvertAction(RValue rValue) {
        super(rValue);
    }

    public abstract ActionType getActionType();
}
