package yajco.model;

import yajco.annotation.Exclude;
import yajco.annotation.Range;
import yajco.model.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

public class OptionalPart extends YajcoModelElement implements NotationPart {
    private List<NotationPart> parts;

    public OptionalPart(@Range(minOccurs = 1) NotationPart[] parts) {
        super(null);
        this.parts = Utilities.asList(parts);
    }

    @Exclude
    public OptionalPart(Object sourceElement) {
        super(sourceElement);
        parts = new ArrayList<NotationPart>();
    }

    //needed for XML binding
    @Exclude
    private OptionalPart() {
        super(null);
    }

    public List<NotationPart> getParts() {
        return parts;
    }

    public void addPart(NotationPart part) {
        parts.add(part);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("Optional: [");
        for (NotationPart notationPart : parts) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(notationPart.toString());
            if (first) {
                first = false;
            }

        }
        sb.append("]");
        return sb.toString();
    }
}
