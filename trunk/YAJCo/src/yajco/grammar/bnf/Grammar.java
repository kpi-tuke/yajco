package yajco.grammar.bnf;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.PatternSupport;
import yajco.grammar.TerminalSymbol;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.Operator;

public class Grammar extends PatternSupport {

	private Hashtable<String, NonterminalSymbol> nonterminals;
	private Hashtable<String, TerminalSymbol> terminals;
	private Hashtable<NonterminalSymbol, Production> productions;
	private NonterminalSymbol startSymbol;
	private Hashtable<TerminalSymbol, String> terminalPool;
	private Hashtable<Integer, List<Alternative>> operatorPool;
	private Hashtable<RangeEntry, NonterminalSymbol> sequencePool;

	public Grammar(NonterminalSymbol startSymbol) {
		this(startSymbol, null);
	}

	public Grammar(NonterminalSymbol startSymbol, List<Pattern> patterns) {
		super(patterns);
		this.nonterminals = new Hashtable<String, NonterminalSymbol>();
		this.terminals = new Hashtable<String, TerminalSymbol>();
		this.productions = new Hashtable<NonterminalSymbol, Production>();
		this.startSymbol = startSymbol;
		this.terminalPool = new Hashtable<TerminalSymbol, String>();
		this.operatorPool = new Hashtable<Integer, List<Alternative>>();
		this.sequencePool = new Hashtable<RangeEntry, NonterminalSymbol>();
	}

	public void addNonterminal(NonterminalSymbol nonterminal) {
		nonterminals.put(nonterminal.getName(), nonterminal);
	}

	public void addTerminal(TerminalSymbol terminal, String regex) {
		terminals.put(terminal.getName(), terminal);
		terminalPool.put(terminal, regex);
	}

	public void addProduction(Production production) {
		NonterminalSymbol lhs = production.getLhs();
		productions.put(lhs, production);
		Operator operatorPattern = (Operator) lhs.getPattern(Operator.class);
		if (operatorPattern == null) {
			return;
		}

		Alternative operatorAlternative = production.getRhs().get(0);
		Integer priority = Integer.valueOf(operatorPattern.getPriority());
		if (!operatorPool.containsKey(priority)) {
			List<Alternative> operators = new ArrayList<Alternative>();
			operators.add(operatorAlternative);
			operatorPool.put(priority, operators);
		} else {
			List<Alternative> operators = operatorPool.get(priority);
			operators.add(operatorAlternative);
		}
	}

	public void addOperatorAlternative(int priority, Alternative alternative) {
		if (alternative == null || priority < 0) {
			return;
		}

		Integer key = Integer.valueOf(priority);
		if (!operatorPool.containsKey(key)) {
			List<Alternative> operators = new ArrayList<Alternative>();
			operators.add(alternative);
			operatorPool.put(key, operators);
		} else {
			List<Alternative> operators = operatorPool.get(key);
			operators.add(alternative);
		}
	}

	public NonterminalSymbol getSequenceNonterminalFor(String name, int min, int max, String sep) {
		RangeEntry entry = new RangeEntry(name, min, max, sep);
		if (sequencePool.containsKey(entry)) {
			return sequencePool.get(entry);
		}

		return null;
	}

	public void addSequence(String name, int min, int max, String sep, NonterminalSymbol nonterminal) {
		sequencePool.put(new RangeEntry(name, min, max, sep), nonterminal);
	}

	public Hashtable<String, NonterminalSymbol> getNonterminals() {
		return nonterminals;
	}

	public NonterminalSymbol getNonterminal(String name) {
		if (nonterminals.containsKey(name)) {
			return nonterminals.get(name);
		}

		return null;
	}

	public Hashtable<Integer, List<Alternative>> getOperatorPool() {
		return operatorPool;
	}

	public Hashtable<NonterminalSymbol, Production> getProductions() {
		return productions;
	}

	public Production getProduction(NonterminalSymbol lhs) {
		if (productions.containsKey(lhs)) {
			return productions.get(lhs);
		}

		return null;
	}

	public NonterminalSymbol getStartSymbol() {
		return startSymbol;
	}

	public Hashtable<TerminalSymbol, String> getTerminalPool() {
		return terminalPool;
	}

	public Hashtable<String, TerminalSymbol> getTerminals() {
		return terminals;
	}

	public TerminalSymbol getTerminal(String regex) {
		for (TerminalSymbol t : terminals.values()) {
			if (terminalPool.get(t).equals(regex)) {
				return t;
			}
		}

		return null;
	}

	private static class RangeEntry {

		private String name;
		private int minOccurs;
		private int maxOccurs;
		private String separator;

		public RangeEntry(String name, int minOccurs, int maxOccurs, String separator) {
			this.name = name;
			this.minOccurs = minOccurs;
			this.maxOccurs = maxOccurs;
			this.separator = separator;
		}

		public String getName() {
			return name;
		}

		public int getMaxOccurs() {
			return maxOccurs;
		}

		public int getMinOccurs() {
			return minOccurs;
		}

		public String getSeparator() {
			return separator;
		}

		@Override
		public String toString() {
			String sep = this.separator != null ? this.separator : "";
			return name + "(from " + minOccurs + " to " + maxOccurs + " sep '" + sep + "')";
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
			final RangeEntry other = (RangeEntry) obj;
			return toString().equals(other.toString());
		}
	}
}
