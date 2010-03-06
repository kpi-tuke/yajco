package tuke.pargen.patterns;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import tuke.pargen.GeneratorException;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.FactoryMethod;
import tuke.pargen.model.ModelElement;
import tuke.pargen.patterns.filters.NonAbstractClassFilter;

public class EmptyClassPattern extends NonAbstractClassFilter implements Pattern {

	private static final String EMPTY_CLASS_ERR = "Class '%s' has no useable constructors or factory methods! Please add some public constructor or some method annotated with @FactoryMethod annotation.";

	public void test(ModelElement element, ProcessingEnvironment processingEnv) throws GeneratorException {
		List<ExecutableElement> alternatives = new ArrayList<ExecutableElement>();

		for (Element e : element.getTypeElement().getEnclosedElements()) {
			boolean isConstructor = e.getKind() == ElementKind.CONSTRUCTOR && e.getModifiers().contains(Modifier.PUBLIC) && e.getAnnotation(Exclude.class) == null;
			boolean isFactoryMethod = e.getKind() == ElementKind.METHOD && e.getModifiers().contains(Modifier.PUBLIC) && e.getAnnotation(FactoryMethod.class) != null && e.getAnnotation(Exclude.class) == null;
			if (isConstructor || isFactoryMethod) {
				alternatives.add((ExecutableElement) e);
			}
		}

		if (alternatives.size() > 0) {
			return;
		}

		TypeMirror classType = element.getTypeElement().asType();
		throw new GeneratorException(String.format(EMPTY_CLASS_ERR, classType.toString()));
	}
}
