package yajco.model;

import yajco.annotation.Exclude;
import yajco.annotation.Range;
import yajco.model.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

public class UnorderedParamPart extends YajcoModelElement implements CompoundNotationPart {
    private List<NotationPart> parts;

    public UnorderedParamPart(@Range(minOccurs = 1) NotationPart[] parts) {
        super(null);
        this.parts = Utilities.asList(parts);
    }

    @Exclude
    public UnorderedParamPart(Object sourceElement) {
        super(sourceElement);
        parts = new ArrayList<NotationPart>();
    }

    //needed for XML binding
    @Exclude
    private UnorderedParamPart() {
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
        sb.append("Unordered param: [");
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
