package tuke.pargen.javacc.model;

public class OneOrMany extends RepeatingExpansion {

	public OneOrMany(String decl, String code, String lookahead, Expansion expansion) {
		super(decl, code, lookahead, expansion);
	}

	public OneOrMany(String decl, String code, Expansion expansion) {
		this(decl, code, null, expansion);
	}

	public OneOrMany(Expansion expansion) {
		this(null, null, expansion);
	}

	@Override
	public String generateExpansion(int level) {
		return spaces(level) + "( " + generateLookahead() + " \n" +
				getExpansion().generateExpansion(level + 1) +
				spaces(level) + ")+\n" +
				spaces(level) + generateCode() + "\n";
	}

	@Override
	public ExpansionType getType() {
		return ExpansionType.ONE_OR_MANY;
	}

	@Override
	public String toString() {
		return "(" + getExpansion().toString() + ")+";
	}

	@Override
	protected int getShortestLength() {
		return getExpansion().getShortestLength();
	}
}
