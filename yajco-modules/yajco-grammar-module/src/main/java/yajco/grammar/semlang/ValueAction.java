package yajco.grammar.semlang;

public abstract class ValueAction extends Action {

    private final LValue lValue;
    private final RValue rValue;

    public ValueAction(LValue lValue, RValue rValue) {
        Utilities.checkForNullPointer(lValue);
        Utilities.checkForNullPointer(rValue);

        this.lValue = lValue;
        this.rValue = rValue;
    }

    public LValue getLValue() {
        return lValue;
    }

    public RValue getRValue() {
        return rValue;
    }

    public abstract ActionType getActionType();
}
