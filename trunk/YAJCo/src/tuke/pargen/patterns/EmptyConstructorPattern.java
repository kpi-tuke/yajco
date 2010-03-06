package tuke.pargen.patterns;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import tuke.pargen.GeneratorException;
import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.model.ModelElement;
import tuke.pargen.patterns.filters.NonAbstractClassFilter;

public class EmptyConstructorPattern extends NonAbstractClassFilter implements Pattern {

	private static final String EMPTY_CONSTRUCTOR_ERR = "Element '%s' contains empty constructor and no annotations are assigned to it! Add @After, @Before annotations or add some parameters to constructor.";

	@Override
	public void test(ModelElement element, ProcessingEnvironment processingEnv) throws GeneratorException {
		for (ExecutableElement constructor : element.getConstructors()) {
			int numParameters = constructor.getParameters().size();
			if (numParameters > 0) {
				continue;
			}

			boolean hasAnnotation = constructor.getAnnotation(Before.class) != null;
			hasAnnotation |= constructor.getAnnotation(After.class) != null;
			if (hasAnnotation) {
				continue;
			}

			throw new GeneratorException(String.format(EMPTY_CONSTRUCTOR_ERR, element.getTypeElement().asType().toString()));
		}
	}
}
