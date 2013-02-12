package yajco.generator.parsergen.javacc.model;

public class ZeroOrOne extends RepeatingExpansion {

    public ZeroOrOne(String decl, String code, String lookahead, Expansion expansion) {
        super(decl, code, lookahead, expansion);
    }

    public ZeroOrOne(String decl, String code, Expansion expansion) {
        this(decl, code, null, expansion);
    }

    public ZeroOrOne(Expansion expansion) {
        this(null, null, expansion);
    }

    @Override
    public String generateExpansion(int level, boolean withCode) {
        if (withCode) resolveLookahead();
        StringBuilder sb = new StringBuilder();
        sb.append(spaces(level)).append("( ");
        if (withCode) {
            sb.append(generateLookahead()).append(" \n");
        }
        sb.append(getExpansion().generateExpansion(level + 1, withCode));
        sb.append(")?\n");
        if (withCode) {
            sb.append(spaces(level)).append(generateCode()).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public ExpansionType getType() {
        return ExpansionType.ZERO_OR_ONE;
    }

    @Override
    public String toString() {
        return "(" + getExpansion().toString() + ")?";
    }

    @Override
    protected int getShortestLength() {
        return 0;
    }
}
