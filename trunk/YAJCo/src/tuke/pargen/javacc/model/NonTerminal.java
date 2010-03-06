package tuke.pargen.javacc.model;

import java.util.Set;

public class NonTerminal extends Expansion {

	private final String variable;
	private final String name;

	public NonTerminal(String decl, String code, String name, String variable) {
		super(decl, code, null);
		this.name = name;
		this.variable = variable;
	}

	public NonTerminal(String name, String variable) {
		this(null, null, name, variable);
	}

	public String getVariable() {
		return variable;
	}

	public String getName() {
		return name;
	}

	@Override
	public ExpansionType getType() {
		return ExpansionType.NONTERMINAL;
	}

	@Override
	public String generateExpansion(int level) {
		StringBuilder sb = new StringBuilder();
		sb.append(spaces(level));
		if (variable != null) {
			sb.append(variable + " = ");
		}
		sb.append(name + Production.NON_TERMINAL_SUFFIX + "()");
		sb.append(generateCode());
		return sb.toString();
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	protected Set<String> first(int n) {
		return getModel().getProduction(name).getExpansion().first(n);
	}

	@Override
	//TODO: tu by som mal odvadzat asi skor od listov, aby
	// som mal zabezpecenu propagaciu hodnoty
	protected int getShortestLength() {
		Production production = getModel().getProduction(name);
		if (production.getShortestLength() == null) {
			production.setShortestLength(0);
			int length = production.getExpansion().getShortestLength();
			production.setShortestLength(length);
		}
		return production.getShortestLength();
	}
}
