package tuke.pargen.patterns;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import tuke.pargen.GeneratorException;
import tuke.pargen.model.ModelElement;
import tuke.pargen.patterns.filters.AnythingFilter;

public class NonGenericListPattern extends AnythingFilter implements Pattern {

	private static final String NON_GENERIC_LIST_ERR = "Type '%s' contains parameter '%s' of type java.util.List but without generics! Please add generic parameter to list.";

	public void test(ModelElement element, ProcessingEnvironment processingEnv) throws GeneratorException {
		for (ExecutableElement constructor : element.getConstructors()) {
			List<? extends VariableElement> parameters = constructor.getParameters();

			for (VariableElement parameter : parameters) {
				String typeString = parameter.asType().toString();
				if (!typeString.startsWith("java.util.List")) {
					continue;
				}

				if (typeString.indexOf('<') != -1) {
					continue;
				}

				throw new GeneratorException(String.format(NON_GENERIC_LIST_ERR, element.getTypeElement().asType().toString(), parameter.toString()));
			}
		}
	}
}
