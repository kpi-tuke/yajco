package yajco.model;

import java.util.ArrayList;
import java.util.List;
import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Optional;
import yajco.annotation.Range;
import yajco.Utilities;
import yajco.model.pattern.NotationPattern;
import yajco.model.pattern.PatternSupport;

public class Notation extends PatternSupport<NotationPattern> {

    private List<NotationPart> parts;

    public Notation(
            @Range(minOccurs = 1) NotationPart[] parts,
            @Optional @Before("{") @After("}") NotationPattern[] patterns) {
        super(Utilities.asList(patterns), null);
        this.parts = Utilities.asList(parts);
    }

    public Notation(
            @Range(minOccurs = 1) NotationPart[] parts) {
        super(new ArrayList<NotationPattern>(), null);
        this.parts = Utilities.asList(parts);
    }

    @Exclude
    public Notation(Object sourceElement) {
        super(sourceElement);
        parts = new ArrayList<NotationPart>();
    }

    public List<NotationPart> getParts() {
        return parts;
    }

    public void addPart(NotationPart part) {
        parts.add(part);
    }
}
