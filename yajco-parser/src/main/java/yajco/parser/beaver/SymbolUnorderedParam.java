package yajco.parser.beaver;

import beaver.Symbol;

public class SymbolUnorderedParam extends Symbol {
    private final Object value;
    private final String varName;

    public SymbolUnorderedParam(Object value, String varName) {
        this.value = value;
        this.varName = varName;
    }

    public Object getValue() {
        return value;
    }

    public String getVarName() {
        return varName;
    }
}
