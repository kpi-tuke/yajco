package tuke.pargen.javacc.model;

public abstract class CompositeExpansion extends Expansion {
    protected final Expansion[] expansions;

    public CompositeExpansion(String decl, String code, String lookahead, Expansion... expansions) {
        super(decl, code, lookahead);
        this.expansions = expansions;

        for (Expansion expansion : expansions) {
            expansion.setParent(this);
        }
    }

    public Expansion[] getExpansions() {
        return expansions;
    }

    @Override
    public String generateDeclaration() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.generateDeclaration());
        for (Expansion expansion : expansions) {
            sb.append(expansion.generateDeclaration());
        }
        return sb.toString();
    }

    protected String toString(String separator) {
        StringBuilder sb = new StringBuilder();

        if (expansions.length > 1) {
            sb.append("(");
        }

        if (expansions.length == 1) {
            sb.append(expansions[0].toString());
        } else {
            boolean separate = false;
            for (Expansion expansion : expansions) {
                if (separate) {
                    sb.append(separator);
                }
                sb.append(expansion.toString());
                separate = true;
            }
        }

        if (expansions.length > 1) {
            sb.append(")");
        }

        return sb.toString();
    }
}
