package tuke.pargen.javacc.model;

import java.util.HashSet;
import java.util.Set;

public class Sequence extends CompositeExpansion {

	public Sequence(String decl, String code, Expansion... expansions) {
		super(decl, code, null, expansions);
	}

	public Sequence(Expansion... expansions) {
		this(null, null, expansions);
	}

	@Override
	public String generateExpansion(int level) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Expansion expansion : expansions) {
			sb.append(expansion.generateExpansion(level + 1) + "\n");
		}
		sb.append(")");
		sb.append(generateCode() + "\n");
		return sb.toString();
	}

	@Override
	public ExpansionType getType() {
		return ExpansionType.SEQUENCE;
	}

	@Override
	public String toString() {
		return toString(" ");
	}

	@Override
	protected Set<String> first(int n) {
		Set<String> set = new HashSet<String>();

		for (Expansion expansion : getExpansions()) {
			int length = expansion.getShortestLength();
			set.addAll(expansion.first(n));
			n -= length;
			if (n <= 0) {
				return set;
			}
		}
		return set;
	}

	@Override
	protected int getShortestLength() {
		int lenght = 0;
		for (Expansion expansion : getExpansions()) {
			lenght += expansion.getShortestLength();
		}
		return lenght;
	}
}
