package yajco.model.grammar;

import java.util.List;
import yajco.model.pattern.Pattern;
import yajco.model.type.Type;

public abstract class Symbol extends PatternSupport {

	private String name;
	private Type returnType;
	private String varName;

	public Symbol(String name, Type returnType) {
		this(name, returnType, null, null);
	}

	public Symbol(String name, Type returnType, String varName) {
		this(name, returnType, varName, null);
	}

	public Symbol(String name, Type returnType, List<Pattern> patterns) {
		this(name, returnType, null, patterns);
	}

	public Symbol(String name, Type returnType, String varName, List<Pattern> patterns) {
		super(patterns);
		this.name = name != null ? name : "";
		this.returnType = returnType;
		this.varName = varName;
	}

	public String getName() {
		return name;
	}

	public Type getReturnType() {
		return returnType;
	}

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}
}
