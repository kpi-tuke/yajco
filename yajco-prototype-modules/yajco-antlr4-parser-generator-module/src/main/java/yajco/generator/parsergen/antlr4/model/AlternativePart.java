package yajco.generator.parsergen.antlr4.model;

import java.util.List;

public class AlternativePart extends CompositePart {
    public AlternativePart(List<Part> parts) {
        super(parts);
    }

    @Override
    protected String generatePart() {
        StringBuilder sb = new StringBuilder();
        boolean parantheses = (!this.parts.isEmpty() && this.parent != null);

        if (parantheses)
            sb.append("(\n");

        for (int i = 0; i < this.parts.size(); i++) {
            Part part = this.parts.get(i);

            sb.append(Formatting.indent(part.generate(), 1)).append("\n");
            if (i != this.parts.size() - 1) {
                sb.append("|\n");
            }
        }

        if (parantheses)
            sb.append("\n)");

        return sb.toString();
    }
}
