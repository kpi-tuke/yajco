package yajco.generator.parsergen.antlr4.model;

import java.util.List;

public class SequencePart extends CompositePart {
    public SequencePart(List<Part> parts) {
        super(parts);
    }

    @Override
    protected String generatePart() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.parts.size(); i++) {
            Part part = this.parts.get(i);
            boolean parentheses = part instanceof CompositePart;
            if (parentheses)
                sb.append("(\n");
            sb.append(part.generate());
            if (parentheses)
                sb.append(")");
            if (i != this.parts.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
