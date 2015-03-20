package yajco.grammar;

import java.util.List;
import yajco.model.pattern.Pattern;
import yajco.model.type.Type;

public class NonterminalSymbol extends Symbol {

	public NonterminalSymbol(String name, Type returnType) {
		super(name, returnType);
	}

	public NonterminalSymbol(String name, Type returnType, String varName) {
		super(name, returnType, varName);
	}

	public NonterminalSymbol(String name, Type returnType, List<Pattern> patterns) {
		super(name, returnType, patterns);
	}

	public NonterminalSymbol(String name, Type returnType, String varName, List<Pattern> patterns) {
		super(name, returnType, varName, patterns);
	}

	@Override
	public String toString() {
		return "<" + getName() + ">";
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
		final NonterminalSymbol other = (NonterminalSymbol) obj;
		return getName().equals(other.getName());
	}
}
