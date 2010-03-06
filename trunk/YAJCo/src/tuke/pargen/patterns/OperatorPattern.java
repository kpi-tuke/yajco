package tuke.pargen.patterns;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import tuke.pargen.GeneratorException;
import tuke.pargen.annotation.Operator;
import tuke.pargen.model.ModelElement;
import tuke.pargen.patterns.filters.NonAbstractClassFilter;

public class OperatorPattern extends NonAbstractClassFilter implements Pattern {

	private static final String REMOVE_OPERATOR_ERROR = "Element '%s' is not operator! Remove @Operator annotation from this element.";
	private static final String ADD_OPERATOR_ERROR = "Element '%s' seems to be an operator! Add @Operator annotation to this element.";

	@Override
	public void test(ModelElement element, ProcessingEnvironment processingEnv) throws GeneratorException {

		Operator operatorAnnotation = element.getConstructors().get(0).getAnnotation(Operator.class);
		boolean hasOperatorAnnotation = operatorAnnotation != null;
		boolean isOperator = isOperatorElement(element, processingEnv);

		if (!hasOperatorAnnotation && isOperator) {
			throw new GeneratorException(String.format(ADD_OPERATOR_ERROR, element.getTypeElement().asType().toString()));
		}
		if (hasOperatorAnnotation && !isOperator) {
			throw new GeneratorException(String.format(REMOVE_OPERATOR_ERROR, element.getTypeElement().asType().toString()));
		}
	}

	private boolean isOperatorElement(ModelElement element, ProcessingEnvironment processingEnv) {
		// TODO: upravit pre viac konstruktorov
		ExecutableElement constructor = element.getConstructors().get(0);
		List<? extends VariableElement> parameters = constructor.getParameters();
		if (parameters.size() == 0) {
			return false;
		}

		TypeMirror mainType = element.getTypeElement().asType();
		for (VariableElement param : parameters) {
			if (!processingEnv.getTypeUtils().isSubtype(mainType, param.asType())) {
				return false;
			}
		}
		return true;
	}
}
