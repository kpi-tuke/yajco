package tuke.pargen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import tuke.pargen.annotation.Exclude;

public class ModelElement {

	private final TypeElement element;
	private final Set<TypeElement> subClasses;
	private final List<ExecutableElement> constructors;

	public ModelElement(TypeElement element, Set<TypeElement> subClasses) {
		this.element = element;
		this.subClasses = subClasses;
		this.constructors = new ArrayList<ExecutableElement>();

		for (Element e : element.getEnclosedElements()) {
			if (e.getKind() == ElementKind.CONSTRUCTOR && e.getModifiers().contains(Modifier.PUBLIC) && e.getAnnotation(Exclude.class) == null) {
				constructors.add((ExecutableElement) e);
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ModelElement other = (ModelElement) obj;
		return element.equals(other.element);
	}

	@Override
	public int hashCode() {
		return element.hashCode();
	}

	public TypeElement getTypeElement() {
		return element;
	}

	public Set<TypeElement> getSubClasses() {
		return subClasses;
	}

	public List<ExecutableElement> getConstructors() {
		return constructors;
	}

	public boolean isClass() {
		return element.getKind() == ElementKind.CLASS;
	}

	public boolean isInterface() {
		return element.getKind() == ElementKind.INTERFACE;
	}
}
