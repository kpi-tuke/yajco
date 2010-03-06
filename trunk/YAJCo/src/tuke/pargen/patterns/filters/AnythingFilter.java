package tuke.pargen.patterns.filters;

import tuke.pargen.model.ModelElement;

public class AnythingFilter implements PatternFilter {

	public boolean filter(ModelElement element) {
		return true;
	}
}
