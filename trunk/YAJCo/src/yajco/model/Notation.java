package yajco.model;

import java.util.ArrayList;
import java.util.List;
import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Optional;
import tuke.pargen.annotation.Range;
import yajco.Utilities;
import yajco.model.pattern.NotationPattern;
import yajco.model.pattern.PatternSupport;

public class Notation extends PatternSupport<NotationPattern> {

	private List<NotationPart> parts;

	public Notation(
			@Range(minOccurs = 1) NotationPart[] parts,
			@Optional @Before("{") @After("}") NotationPattern[] patterns) {
		super(Utilities.asList(patterns));
		this.parts = Utilities.asList(parts);
	}

	@Exclude
	public Notation() {
		parts = new ArrayList<NotationPart>();
	}

	public List<NotationPart> getParts() {
		return parts;
	}

	public void addPart(NotationPart part) {
		parts.add(part);
	}
}
