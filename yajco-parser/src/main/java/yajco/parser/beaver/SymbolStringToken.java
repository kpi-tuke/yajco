package yajco.parser.beaver;

import beaver.Symbol;

public class SymbolStringToken extends Symbol {

    private String value;

    public SymbolStringToken(String value) {
        this.value = value.substring(1, value.length() - 1)
                .replaceAll("\\\\b", "\b")
                .replaceAll("\\\\r", "\r")
                .replaceAll("\\\\t", "\t")
                .replaceAll("\\\\f", "\f")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\\"", "\"")
                .replaceAll("\\\\\'", "\'")
                .replace("\\\\", "\\");
    }

    public String getStringValue() {
        return value;
    }
}
