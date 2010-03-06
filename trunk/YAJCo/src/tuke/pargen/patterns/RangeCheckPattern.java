package tuke.pargen.patterns;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import tuke.pargen.GeneratorException;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.FactoryMethod;
import tuke.pargen.annotation.Range;
import tuke.pargen.model.ModelElement;
import tuke.pargen.patterns.filters.NonAbstractClassFilter;

public class RangeCheckPattern extends NonAbstractClassFilter implements Pattern {

	private static final String INFINITY_FROM_ERR = "@Range annotation applied to parameter '%s' in method '%s' in class '%s' contains invalid value for parameter from! Parameter from cann't be INFINITY.";
	private static final String TO_LESS_THAN_FROM_ERR = "@Range annotation applied to parameter '%s' in method '%s' in class '%s' contains invalid values for parameters to and from! Parameter to cann't be less than parameter from.";

	public void test(ModelElement element, ProcessingEnvironment processingEnv) throws GeneratorException {
		for (Element e : element.getTypeElement().getEnclosedElements()) {
			boolean isConstructor = e.getKind() == ElementKind.CONSTRUCTOR && e.getModifiers().contains(Modifier.PUBLIC) && e.getAnnotation(Exclude.class) == null;
			boolean isFactoryMethod = e.getKind() == ElementKind.METHOD && e.getModifiers().contains(Modifier.PUBLIC) && e.getAnnotation(FactoryMethod.class) != null && e.getAnnotation(Exclude.class) == null;
			if (!isConstructor && !isFactoryMethod) {
				continue;
			}

			ExecutableElement alternative = (ExecutableElement) e;
			List<? extends VariableElement> parameters = alternative.getParameters();
			for (VariableElement parameter : parameters) {
				Range rangeAnnotation = parameter.getAnnotation(Range.class);
				if (rangeAnnotation == null) {
					continue;
				}

				int from = rangeAnnotation.minOccurs();
				int to = rangeAnnotation.maxOccurs();
				TypeMirror classType = element.getTypeElement().asType();
				if (from == Range.INFINITY) {
					throw new GeneratorException(String.format(INFINITY_FROM_ERR, parameter.getSimpleName().toString(), alternative.getSimpleName().toString(), classType.toString()));
				}
				if (to < from && to != Range.INFINITY) {
					throw new GeneratorException(String.format(TO_LESS_THAN_FROM_ERR, parameter.getSimpleName().toString(), alternative.getSimpleName().toString(), classType.toString()));
				}
			}
		}
	}

	
}
