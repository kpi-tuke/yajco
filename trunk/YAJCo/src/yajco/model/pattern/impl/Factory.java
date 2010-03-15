package yajco.model.pattern.impl;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import yajco.model.pattern.NotationPattern;

public class Factory implements NotationPattern {

	private final String name;

	@Before({"Factory", "("})
	@After(")")
	public Factory(@Before({"method", "="}) String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
