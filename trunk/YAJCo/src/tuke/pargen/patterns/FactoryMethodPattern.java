package tuke.pargen.patterns;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import tuke.pargen.GeneratorException;
import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.FactoryMethod;
import tuke.pargen.model.ModelElement;
import tuke.pargen.patterns.filters.NonAbstractClassFilter;

public class FactoryMethodPattern extends NonAbstractClassFilter implements Pattern {

	private static final String NOT_PUBLIC_ERR = "Method '%s' in class '%s' is not public! Add public modifier or remove @FactoryMethod annotation.";
	private static final String NOT_METHOD_ERR = "Annotation @FactoryMethod can be used only with methods! '%s' in class '%s' is not a method.";
	private static final String BAD_RETURN_TYPE_ERR = "Method '%s' in class '%s' has incorrect return type! Return type must be '%s'.";
	private static final String MISSING_STATIC_MODIFIER_ERR = "Method '%s' in class '%s' must have modifier static! Add static modifier to this method or remove @FactoryMethod annotation from it.";
	private static final String NO_PARAMETERS_ERR = "Factory method '%s' in class '%s' has no parameters and no annotations are assigned to it! Add some parameters to this method or add @After or @Before annotation.";

	public void test(ModelElement element, ProcessingEnvironment processingEnv) throws GeneratorException {
		TypeElement typeElement = element.getTypeElement();
		List<? extends Element> enclosedElements = typeElement.getEnclosedElements();

		for (Element e : enclosedElements) {
			if (e.getAnnotation(FactoryMethod.class) == null || e.getAnnotation(Exclude.class) != null) {
				continue;
			}

			TypeMirror classType = typeElement.asType();
			if (!e.getModifiers().contains(Modifier.PUBLIC)) {
				throw new GeneratorException(String.format(NOT_PUBLIC_ERR, e.getSimpleName().toString(), classType.toString()));
			}
			if (e.getKind() != ElementKind.METHOD) {
				throw new GeneratorException(String.format(NOT_METHOD_ERR, e.getSimpleName().toString(), classType.toString()));
			}

			ExecutableElement methodElement = (ExecutableElement) e;
			if (!methodElement.getReturnType().equals(classType)) {
				throw new GeneratorException(String.format(BAD_RETURN_TYPE_ERR, methodElement.getSimpleName().toString(), classType.toString(), classType.toString()));
			}
			if (!methodElement.getModifiers().contains(Modifier.STATIC)) {
				throw new GeneratorException(String.format(MISSING_STATIC_MODIFIER_ERR, methodElement.getSimpleName().toString(), classType.toString()));
			}

			List<? extends VariableElement> parameters = methodElement.getParameters();
			if (parameters.size() > 0) {
				continue;
			}

			boolean hasAnnotation = methodElement.getAnnotation(Before.class) != null;
			hasAnnotation |= methodElement.getAnnotation(After.class) != null;
			if (!hasAnnotation) {
				throw new GeneratorException(String.format(NO_PARAMETERS_ERR, methodElement.getSimpleName().toString(), classType.toString()));
			}
		}
	}
}
