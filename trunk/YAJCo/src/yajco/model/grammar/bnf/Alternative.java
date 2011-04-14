package yajco.model.grammar.bnf;

import java.util.ArrayList;
import java.util.List;
import yajco.model.grammar.PatternSupport;
import yajco.model.grammar.Symbol;
import yajco.model.grammar.semlang.Action;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.Associativity;
import yajco.model.pattern.impl.Operator;

public class Alternative extends PatternSupport {

	private List<Symbol> symbols;
	private List<Action> actions;

	public Alternative() {
		this(null, null, null);
	}

	public Alternative(List<Symbol> symbols) {
		this(symbols, null, null);
	}

	public Alternative(List<Symbol> symbols, List<Action> actions, List<Pattern> patterns) {
		super(patterns);
		this.symbols = symbols != null ? symbols : new ArrayList<Symbol>();
		this.actions = actions != null ? actions : new ArrayList<Action>();
	}

	public void addSymbol(Symbol symbol) {
		if (symbol != null) {
			symbols.add(symbol);
		}
	}

	public void addSymbols(List<Symbol> symbols) {
		if (symbols != null && symbols.size() > 0) {
			this.symbols.addAll(symbols);
		}
	}

	public void addAction(Action action) {
		if (action != null) {
			actions.add(action);
		}
	}

	public void addActions(List<Action> actions) {
		if (actions != null && actions.size() > 0) {
			this.actions.addAll(actions);
		}
	}

	public Symbol getSymbol(String name) {
		for (Symbol symbol : symbols) {
			if (symbol.getName().equals(name)) {
				return symbol;
			}
		}

		return null;
	}

	public boolean isOperatorAlternative() {
		return getPattern(Operator.class) != null;
	}

	public List<Symbol> getSymbols() {
		return symbols;
	}

	public List<Action> getActions() {
		return actions;
	}

	public int getPriority() {
		Operator opPattern = (Operator) getPattern(Operator.class);
		return opPattern != null ? opPattern.getPriority() : -1;
	}

	public Associativity getAssociativity() {
		Operator opPattern = (Operator) getPattern(Operator.class);
		return opPattern != null ? opPattern.getAssociativity() : null;
	}

	public boolean hasSymbol(String name) {
		return getSymbol(name) != null;
	}

	public boolean isEmpty() {
		return symbols.isEmpty();
	}

	@Override
	public String toString() {
		if (isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for (Symbol s : symbols) {
			builder.append(s.toString());
			builder.append(" ");
		}
		builder.setLength(builder.length() - 1);

		return builder.toString();
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Alternative other = (Alternative) obj;
		return toString().equals(other.toString());
	}
}
