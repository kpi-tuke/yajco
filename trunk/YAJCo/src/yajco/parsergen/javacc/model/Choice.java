package yajco.parsergen.javacc.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Choice extends CompositeExpansion {

	public Choice(String decl, String code, String lookahead, Expansion... expansions) {
		super(decl, code, lookahead, expansions);
	}

	public Choice(String decl, String code, Expansion... expansions) {
		this(decl, code, null, expansions);
	}

	public Choice(Expansion... expansions) {
		this(null, null, expansions);
	}

	@Override
	public String generateExpansion(int level) {
		resolveLookahead();

		StringBuilder sb = new StringBuilder();
		sb.append("(");
		boolean separate = false;
		for (Expansion expansion : expansions) {
			if (separate) {
				sb.append(spaces(level) + " |\n");
			} else {
				separate = true;
			}

			sb.append(expansion.generateLookahead());
			sb.append(expansion.generateExpansion(level + 1) + "\n");
		}
		sb.append(")");
		sb.append(generateCode() + "\n");
		return sb.toString();
	}

	@Override
	public ExpansionType getType() {
		return ExpansionType.CHOICE;
	}

	@Override
	public String toString() {
		return toString(" | ");
	}

	@Override
	protected Set<String> first(int n) {
		Set<String> set = new HashSet<String>();

		for (Expansion expansion : expansions) {
			set.addAll(expansion.first(n));
		}
		return set;
	}

	@Override
	protected int getShortestLength() {
		int min = expansions[0].getShortestLength();
		for (Expansion expansion : expansions) {
			int lenght = expansion.getShortestLength();
			if (lenght < min) {
				min = lenght;
			}
		}
		return min;
	}

	private void resolveLookahead() {
		int size = expansions.length;
		for (int i = 0; i < size; i++) {
			Expansion expansion1 = expansions[i];
			Set<String> set1 = expansion1.first(1);
			for (int j = i + 1; j < size; j++) {
				Expansion expansion2 = expansions[j];
				Set<String> set2 = expansion2.first(1);
				if (!Collections.disjoint(set1, set2)) {
					int lookahead = findLookahead(expansion1, expansion2, 1);
					if (expansion1.getShortestLength() < expansion2.getShortestLength()) {
						expansions[j] = expansion1;
						expansions[i] = expansion2;
						expansion2.setLookahead(String.valueOf(lookahead));
					} else {
						expansion1.setLookahead(String.valueOf(lookahead));
					}
				}
			}
		}
	}

	private int findLookahead(Expansion expansion1, Expansion expansion2, int firstLength) {
		Set<String> set1 = expansion1.first(firstLength);
		Set<String> set2 = expansion2.first(firstLength);
		System.out.println(">>>> " + getProduction().getName() + " len: " + firstLength + " " + set1 + " " + set2);
		if (!Collections.disjoint(set1, set2)) {
			return findLookahead(expansion1, expansion2, firstLength + 1);
		}
		return firstLength;
	}
}
