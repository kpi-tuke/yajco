package yajco.grammar;

import java.util.List;
import yajco.model.pattern.Pattern;
import yajco.model.type.Type;

public abstract class Symbol extends PatternSupport implements Cloneable {

    private final String name;
    private final Type returnType;
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

    /**
     * @return shallow copy
     * @throws RuntimeException if {@link CloneNotSupportedException} was thrown
     */
    @Override
    public Symbol clone() throws RuntimeException {
        try {
            return (Symbol) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Returns a {@link #clone() shallow copy} of this {@code Symbol} with the varName altered.
     *
     * @return a {@code Symbol} based on this symbol with the requested varName, not null
     *
     * Note: This method is in the style of the convention of Lombok's {@code @With}
     * and the {@code with*()} methods in {@code java.time}.
     */
    public Symbol withVarName(String newVarName) {
        final Symbol clone = clone();
        clone.setVarName(newVarName);
        return clone;
    }
}
