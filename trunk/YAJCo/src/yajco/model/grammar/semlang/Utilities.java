package yajco.model.grammar.semlang;

import yajco.model.grammar.Symbol;

public final class Utilities {

	public static void checkForNullPointer(Object value) {
		if (value == null) {
			throw new NullPointerException("Parameter 'value' cann't be null!");
		}
	}

	public static void checkForEmptyString(String value) {
		if (value.equals("")) {
			throw new IllegalArgumentException("Parameter 'value' cann't be empty!");
		}
	}

	public static void checkForNullOrEmptyString(String value) {
		checkForNullPointer(value);
		checkForEmptyString(value);
	}

	public static void checkInputSymbol(Symbol symbol) {
		checkForNullPointer(symbol);
		checkForNullOrEmptyString(symbol.getVarName());
	}
}
