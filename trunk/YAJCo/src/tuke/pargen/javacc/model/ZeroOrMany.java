package tuke.pargen.javacc.model;

public class ZeroOrMany extends RepeatingExpansion {

	public ZeroOrMany(String decl, String code, String lookahead, Expansion expansion) {
		super(decl, code, lookahead, expansion);
	}

	public ZeroOrMany(String decl, String code, Expansion expansion) {
		this(decl, code, null, expansion);
	}

	public ZeroOrMany(Expansion expansion) {
		this(null, null, expansion);
	}

	@Override
	public String generateExpansion(int level) {
		return spaces(level) + "( " + generateLookahead() + " \n" +
				getExpansion().generateExpansion(level + 1) +
				spaces(level) + ")*\n" +
				spaces(level) + generateCode() + "\n";
	}

	@Override
	public ExpansionType getType() {
		return ExpansionType.ZERO_OR_MANY;
	}

	@Override
	public String toString() {
		return "(" + getExpansion().toString() + ")*";
	}

	@Override
	protected int getShortestLength() {
		return 0;
	}
}
