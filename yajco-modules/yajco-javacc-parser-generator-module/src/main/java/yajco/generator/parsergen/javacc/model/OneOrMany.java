package yajco.generator.parsergen.javacc.model;

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
    public String generateExpansion(int level, boolean withCode) {
        if (withCode) resolveLookahead();
        StringBuilder sb = new StringBuilder();
        sb.append(spaces(level));
        sb.append("( ");
        if (withCode) {
            sb.append(generateLookahead());
            sb.append(" \n");
        }
        sb.append(getExpansion().generateExpansion(level + 1, withCode));
        sb.append(spaces(level)).append(")+\n");
        if (withCode) {
            sb.append(spaces(level)).append(generateCode()).append("\n");
        }
        return sb.toString();
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
