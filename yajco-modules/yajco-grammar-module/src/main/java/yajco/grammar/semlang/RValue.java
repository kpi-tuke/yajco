package yajco.grammar.semlang;

import yajco.grammar.Symbol;

public class RValue extends LValue {

    private final Action action;
    private final Object literalValue;
    private final boolean isLiteral;

    public RValue(Symbol symbol) {
        super(symbol);

        this.action = null;
        this.literalValue = null;
        this.isLiteral = false;
    }

    public RValue(Action action) {
        Utilities.checkForNullPointer(action);

        this.action = action;
        this.literalValue = null;
        this.isLiteral = false;
    }

    public RValue(String varName) {
        super(varName);

        this.action = null;
        this.literalValue = null;
        this.isLiteral = false;
    }

    /**
     * Constructor for literal values (boolean, int, string, etc.)
     */
    public RValue(Object literalValue) {
        super();

        this.action = null;
        this.literalValue = literalValue;
        this.isLiteral = true;
    }

    public Action getAction() {
        return action;
    }

    public Object getLiteralValue() {
        return literalValue;
    }

    public boolean isLiteral() {
        return isLiteral;
    }
}
