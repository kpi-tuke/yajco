package yajco.annotation.processor;

import yajco.generator.GeneratorException;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.PatternSupport;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class PatternMapper {
    private final ProcessingEnvironment processingEnv;

    public PatternMapper(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    //TOTO je klucove pre otvorenost procesora, kopiruje vzor uvedeny v anotacii do modelu
    //TODO - navrhujem doplnit kontrolu podla typu vzoru
    /**
     * Finds if language model elements annotation is annotated with @MapsTo annotations.
     *
     * @param element Language model element.
     * @param <T>
     * @return If elements annotation contains annotated with @MapsTo annotations.
     */
    public <T extends Pattern> boolean hasPatternAnnotations(Element element) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            Element annotationElement = processingEnv.getTypeUtils().asElement(am.getAnnotationType());
            MapsTo mapsTo = annotationElement.getAnnotation(MapsTo.class);
            if (mapsTo != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds patterns from annotations to language.
     *
     * @param element Language model element.
     * @param patternSupport Language concept.
     * @param <T>
     */
    public <T extends Pattern> void addPatternsFromAnnotations(Element element, PatternSupport<T> patternSupport) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            Element annotationElement = processingEnv.getTypeUtils().asElement(am.getAnnotationType());
            MapsTo mapsTo = annotationElement.getAnnotation(MapsTo.class);
            if (mapsTo != null) {
                System.out.println("Processing annotation pattern: " + am);
                String mapsToClass = mapsTo.value();
                patternSupport.addPattern((T) createObjectFromAnnotation(mapsToClass, am));
            }
        }
    }

    /**
     * Creates object from annotation.
     *
     * @param mapsToClass Class that reflects annotation function.
     * @param am Annotation
     * @return Pattern
     */
    private Pattern createObjectFromAnnotation(String mapsToClass, AnnotationMirror am) {
        try {
            Class<? extends Pattern> clazz = (Class<? extends Pattern>) Class.forName(mapsToClass);
            Pattern pattern = clazz.getConstructor().newInstance();
            // Copy from annotation into created object, according to the same names of annotation property and field.
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils().getElementValuesWithDefaults(am);
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                String name = entry.getKey().getSimpleName().toString();
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                field.set(pattern, convertValue(field.getType(), entry.getValue().getValue()));
            }
            return pattern;
        } catch (Exception e) {
            // TODO: upravit vypis
            throw new GeneratorException("Cannot instantiate class for @MapsTo, class " + mapsToClass, e);
        }
    }

    private Object convertValue(Class<?> targetType, Object value) throws ClassNotFoundException {
        if (value instanceof VariableElement) {
            VariableElement enumField = (VariableElement) value;
            return Enum.valueOf((Class<? extends Enum>) Class.forName(enumField.asType().toString()), enumField.getSimpleName().toString());
        }
        if (targetType.isArray() && value instanceof List<?>) {
            Class<?> componentType = targetType.getComponentType();
            List<?> values = (List<?>) value;
            Object array = Array.newInstance(componentType, values.size());
            for (int i = 0; i < values.size(); i++) {
                Object item = values.get(i);
                if (item instanceof AnnotationValue) {
                    item = ((AnnotationValue) item).getValue();
                }
                Array.set(array, i, convertValue(componentType, item));
            }
            return array;
        }
        return value;
    }
}
