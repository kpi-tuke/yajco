package yajco.grammar.bnf;

import java.util.ArrayList;
import java.util.List;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.PatternSupport;
import yajco.model.pattern.Pattern;

public class Production extends PatternSupport {

	private NonterminalSymbol lhs;
	private List<Alternative> rhs;

	public Production(NonterminalSymbol lhs) {
		this(lhs, null, null);
	}

	public Production(NonterminalSymbol lhs, List<Alternative> rhs) {
		this(lhs, rhs, null);
	}

	public Production(NonterminalSymbol lhs, List<Alternative> rhs, List<Pattern> patterns) {
		super(patterns);
		this.lhs = lhs;
		this.rhs = rhs != null ? rhs : new ArrayList<Alternative>();
	}

	public void addAlternative(Alternative alternative) {
		if (alternative != null) {
			rhs.add(alternative);
		}
	}

	public NonterminalSymbol getLhs() {
		return lhs;
	}

	public List<Alternative> getRhs() {
		return rhs;
	}

	@Override
	public String toString() {
		if (rhs.isEmpty()) {
			return lhs.toString() + " ::= ";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(lhs.toString());
		builder.append(" ::= ");
		for (Alternative a : rhs) {
			builder.append(a.toString());
			builder.append(" | ");
		}
		builder.setLength(builder.length() - 3);

		return builder.toString();
	}

	@Override
	public int hashCode() {
		// identita pravidla sa urcuje iba v ramci danej gramatiky, v ktorej sa pravidlo nachadza, takze
		// preto sa hashCode pocita iba z neterminalu nachadzahuceho sa na lavej strane pravidla a nie zo
		// vsetkych alternativ pravidla
		return lhs.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Production other = (Production) obj;
		return lhs.equals(other.lhs);
	}
}
