package yajco.model;

import yajco.annotation.Exclude;
import yajco.annotation.Range;
import yajco.model.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a mixed repetition part in the notation.
 *
 * MixedRepetitionPart allows interleaving elements from multiple collection parameters
 * in the concrete syntax. During parsing, elements are automatically sorted into their
 * corresponding collections based on type.
 *
 * Each part in the parts list represents one collection parameter that participates
 * in the mixed repetition.
 */
public class MixedRepetitionPart extends YajcoModelElement implements CompoundNotationPart {
    private List<NotationPart> parts;

    public MixedRepetitionPart(@Range(minOccurs = 1) NotationPart[] parts) {
        super(null);
        this.parts = Utilities.asList(parts);
    }

    @Exclude
    public MixedRepetitionPart(Object sourceElement) {
        super(sourceElement);
        parts = new ArrayList<NotationPart>();
    }

    //needed for XML binding
    @Exclude
    private MixedRepetitionPart() {
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
        sb.append("Mixed repetition: [");
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
