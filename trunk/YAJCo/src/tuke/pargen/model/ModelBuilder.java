package tuke.pargen.model;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import tuke.pargen.annotation.Exclude;

public class ModelBuilder {

	private final RoundEnvironment roundEnvironment;
	private final ProcessingEnvironment processingEnvironment;
	private final TypeElement rootElement;
	private final Set<ModelElement> modelElements;
	private final Set<TypeElement> knownTypes;
	private final Set<TypeElement> processedTypes;

	public ModelBuilder(RoundEnvironment roundEnvironment, ProcessingEnvironment processingEnvironment, TypeElement rootElement) {
		this.roundEnvironment = roundEnvironment;
		this.processingEnvironment = processingEnvironment;
		this.rootElement = rootElement;
		modelElements = new HashSet<ModelElement>();
		knownTypes = new HashSet<TypeElement>();
		processedTypes = new HashSet<TypeElement>();

		buildModel();
	}

	private void buildModel() {
		for (Element e : roundEnvironment.getRootElements()) {
			ElementKind kind = e.getKind();
			if (kind == ElementKind.CLASS || kind == ElementKind.INTERFACE) {
				knownTypes.add((TypeElement) e);
			}
		}

		ModelElement rootModelElement = processElement(rootElement);
		modelElements.add(rootModelElement);

		processModelElement(rootModelElement);
	}

	private void processModelElement(ModelElement element) {
		if (processedTypes.contains(element.getTypeElement())) {
			return;
		}
		processedTypes.add(element.getTypeElement());
		// TODO: Optimalizacia
		for (ExecutableElement constructor : element.getConstructors()) {
			for (VariableElement param : constructor.getParameters()) {
				Element paramElement = processingEnvironment.getTypeUtils().asElement(param.asType());
				if (paramElement == null) {
					continue;
				}
				if (!knownTypes.contains(paramElement)) {
					continue;
				}

				ElementKind kind = paramElement.getKind();
				if (kind == ElementKind.CLASS || kind == ElementKind.INTERFACE) {
					ModelElement modelElement = processElement((TypeElement) paramElement);
					modelElements.add(modelElement);
					processModelElement(modelElement);
				}
			}
		}

		for (TypeElement subType : element.getSubClasses()) {
			ModelElement modelElement = processElement(subType);
			modelElements.add(modelElement);

			processModelElement(modelElement);
		}
	}

	private ModelElement processElement(TypeElement element) {
		Set<TypeElement> subclasses = getDirectSubclassesFor(element);
		return new ModelElement(element, subclasses);
	}

	private Set<TypeElement> getDirectSubclassesFor(TypeElement element) {
		Set<TypeElement> subclasses = new HashSet<TypeElement>();
		for (Element e : roundEnvironment.getRootElements()) {
			if (isDirectSubtype(element, e)) {
				subclasses.add((TypeElement) e);
			}
		}

		return subclasses;
	}

	private boolean isDirectSubtype(TypeElement supertype, Element element) {
		if (element.getAnnotation(Exclude.class) == null) {
			if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
				TypeMirror superType = supertype.asType();
				if (processingEnvironment.getTypeUtils().isSameType(((TypeElement) element).getSuperclass(), superType)) {
					return true;
				}
				for (TypeMirror type : ((TypeElement) element).getInterfaces()) {
					if (processingEnvironment.getTypeUtils().isSameType(type, superType)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Set<ModelElement> getModelElements() {
		return modelElements;
	}
}
