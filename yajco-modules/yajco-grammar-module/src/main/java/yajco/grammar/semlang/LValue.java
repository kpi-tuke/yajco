package yajco.grammar.semlang;

import yajco.grammar.Symbol;

public class LValue {

    private final Symbol symbol;
    private final String varName;

    public LValue(Symbol symbol) {
        Utilities.checkInputSymbol(symbol);

        this.symbol = symbol;
        this.varName = null;
    }

    public LValue(String varName) {
        Utilities.checkForNullOrEmptyString(varName);

        this.symbol = null;
        this.varName = varName;
    }

    protected LValue() {
        this.symbol = null;
        this.varName = null;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public String getVarName() {
        return varName;
    }
}
