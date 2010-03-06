package tuke.pargen.patterns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import tuke.pargen.GeneratorException;
import tuke.pargen.model.ModelElement;

public class PatternMatcher {

	private static final PatternMatcher instance = new PatternMatcher();
	private ProcessingEnvironment processingEnv;
	private final Set<Pattern> patterns;

	private PatternMatcher() {
		patterns = new HashSet<Pattern>();
		patterns.add(new EmptyClassPattern());
		patterns.add(new OperatorPattern());
		patterns.add(new EmptyConstructorPattern());
		patterns.add(new NonGenericListPattern());
		patterns.add(new NonGenericSetPattern());
		patterns.add(new FactoryMethodPattern());
		patterns.add(new RangeCheckPattern());
	}

	public void testModel(Set<ModelElement> modelElements) throws GeneratorException {
		List<GeneratorException> exceptions = new ArrayList<GeneratorException>();

		for (ModelElement element : modelElements) {
			for (Pattern pattern : patterns) {
				try {
					if (pattern.filter(element)) {
						pattern.test(element, processingEnv);
					}
				} catch (GeneratorException e) {
					exceptions.add(e);
				}
			}
		}

		if (exceptions.size() > 0) {
			StringBuilder builder = new StringBuilder();

			for (GeneratorException e : exceptions) {
				builder.append(e.getMessage());
				builder.append("\n");
			}

			builder.setLength(builder.length() - 1);
			throw new GeneratorException(builder.toString());
		}
	}

	public void setProcessingEnv(ProcessingEnvironment processingEnv) {
		this.processingEnv = processingEnv;
	}

	public static PatternMatcher getInstance() {
		return instance;
	}
}
