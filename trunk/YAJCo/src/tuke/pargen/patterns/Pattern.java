package tuke.pargen.patterns;

import javax.annotation.processing.ProcessingEnvironment;
import tuke.pargen.GeneratorException;
import tuke.pargen.model.ModelElement;
import tuke.pargen.patterns.filters.PatternFilter;

public interface Pattern extends PatternFilter {

	public void test(ModelElement element, ProcessingEnvironment processingEnv) throws GeneratorException;
}
