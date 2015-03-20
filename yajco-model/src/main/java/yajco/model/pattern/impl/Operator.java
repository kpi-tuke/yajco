package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.ConceptPattern;

public class Operator extends ConceptPattern {

	private int priority;
	private Associativity associativity;

	@Before({"Operator", "("})
	@After(")")
	public Operator(@Before({"priority", "="}) int intValue) {
		super(null);
		this.priority = intValue;
	}

	@Before({"Operator", "("})
	@After(")")
	public Operator(
			@Before({"priority", "="}) int intValue,
			@Before({",", "associativity", "="}) Associativity associativity) {
		super(null);
		this.priority = intValue;
		this.associativity = associativity;
	}

	@Exclude
	public Operator() {
		super(null);
	}

	@Exclude
	public Operator(int priority, Associativity associativity, Object sourceElement) {
		super(sourceElement);
		this.priority = priority;
		this.associativity = associativity;
	}

	public int getPriority() {
		return priority;
	}

	public Associativity getAssociativity() {
		return associativity;
	}
}
