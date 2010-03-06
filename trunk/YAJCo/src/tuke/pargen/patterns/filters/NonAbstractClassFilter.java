package tuke.pargen.patterns.filters;

import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import tuke.pargen.model.ModelElement;

public abstract class NonAbstractClassFilter implements PatternFilter {

	public boolean filter(ModelElement element) {
		ElementKind kind = element.getTypeElement().getKind();
		Set<Modifier> modifiers = element.getTypeElement().getModifiers();

		return kind == ElementKind.CLASS && !modifiers.contains(Modifier.ABSTRACT);
	}
}
