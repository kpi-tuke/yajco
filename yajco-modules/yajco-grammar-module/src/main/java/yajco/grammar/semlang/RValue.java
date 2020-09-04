package yajco.grammar.semlang;

import yajco.grammar.Symbol;

public class RValue extends LValue {

    private final Action action;

    public RValue(Symbol symbol) {
        super(symbol);

        this.action = null;
    }

    public RValue(Action action) {
        Utilities.checkForNullPointer(action);

        this.action = action;
    }

    public RValue(String varName) {
        super(varName);

        this.action = null;
    }

    public Action getAction() {
        return action;
    }
}
