package yajco.parsergen.javacc.model;

public class ZeroOrOne extends RepeatingExpansion {

	public ZeroOrOne(String decl, String code, String lookahead, Expansion expansion) {
		super(decl, code, lookahead, expansion);
	}

	public ZeroOrOne(String decl, String code, Expansion expansion) {
		this(decl, code, null, expansion);
	}

	public ZeroOrOne(Expansion expansion) {
		this(null, null, expansion);
	}

	@Override
	public String generateExpansion(int level) {
		return spaces(level) + "( " + generateLookahead() + " \n" +
				getExpansion().generateExpansion(level + 1) +
				spaces(level) + ")?\n" +
				spaces(level) + generateCode() + "\n";
	}

	@Override
	public ExpansionType getType() {
		return ExpansionType.ZERO_OR_ONE;
	}

	@Override
	public String toString() {
		return "(" + getExpansion().toString() + ")?";
	}

	@Override
	protected int getShortestLength() {
		return 0;
	}
}
