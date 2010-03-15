package yajco.model;

import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Token;

public class TokenDef {

	private final String name;
	private final String regexp;

	public TokenDef(String name, @Before("=") @Token("STRING_VALUE") String regexp) {
		this.name = name;
		this.regexp = regexp;
	}

	public String getName() {
		return name;
	}

	public String getRegexp() {
		return regexp;
	}
}
