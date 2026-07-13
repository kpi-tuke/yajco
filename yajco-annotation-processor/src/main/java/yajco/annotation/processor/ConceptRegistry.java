package yajco.annotation.processor;

import org.jspecify.annotations.Nullable;
import yajco.annotation.Exclude;
import yajco.model.Concept;
import yajco.model.Language;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Central registry for language concepts discovered during annotation processing.
 * The registry is intentionally stateful for a single parser/model-building run and should
 * be created per language instance.
 */
public class ConceptRegistry {
    private final Language language;
    private final Set<? extends Element> rootElements;
    private final Set<String> excludes;
    private final Types typeUtils;

    /**
     * Set for concepts imported from previous JARs and needed for full analysis.
     */
    private final Set<Concept> conceptsToProcess = new HashSet<>();

    public ConceptRegistry(Language language, Set<? extends Element> rootElements, Set<String> excludes, Types typeUtils) {
        this.language = language;
        this.rootElements = rootElements;
        this.excludes = excludes;
        this.typeUtils = typeUtils;
    }

    /**
     * Registers concepts imported from external language definitions.
     * Imported concepts are added to the current language and marked for potential reprocessing.
     */
    public void registerImportedConcepts(Collection<Concept> imported) {
        conceptsToProcess.addAll(imported);
        language.getConcepts().addAll(imported);
    }

    /**
     * Returns an existing concept for the given type or creates a new one when missing.
     * Optionally assign a parent concept.
     *
     * @return resolved or newly created concept, or {@code null} when the type is excluded
     */
    public @Nullable Concept getOrCreate(TypeElement typeElement, @Nullable Concept parent) {
        if (shouldSkip(typeElement)) {
            return null;
        }

        String name = normalizedName(typeElement);
        Concept concept = language.getConcept(name);
        if (concept == null) {
            concept = new Concept(name, typeElement);
            language.addConcept(concept);
        }

        if (parent != null) {
            concept.setParent(parent);
        }

        return concept;
    }

    /**
     * Returns whether the given type element is excluded from processing.
     */
    public boolean shouldSkip(TypeElement typeElement) {
        return excludes.contains(typeElement.getQualifiedName().toString());
    }

    /**
     * Finds direct subtypes of the given supertype among round root elements.
     */
    public Iterable<TypeElement> directSubtypesOf(TypeElement superType) {
        Set<TypeElement> result = new LinkedHashSet<>();
        for (Element element : rootElements) {
            if (isDirectSubtype(superType, element)) {
                result.add((TypeElement) element);
            }
        }
        return result;
    }

    /**
     * Resolves a concept only if the type is known in the current processing scope.
     *
     * @return matching concept, or {@code null} if the type is unknown or out of scope
     */
    public @Nullable Concept resolveKnown(TypeElement typeElement) {
        if (typeElement == null || !isKnownClass(typeElement)) {
            return null;
        }

        Concept byNormalized = language.getConcept(normalizedName(typeElement));
        if (byNormalized != null) {
            return byNormalized;
        }

        return language.getConcept(typeElement.getQualifiedName().toString());
    }

    /**
     * Marks an imported concept as consumed (already reprocessed) in this run.
     *
     * @return {@code true} if the concept was pending and is now consumed
     */
    public boolean consumeImportedConcept(Concept concept) {
        return conceptsToProcess.remove(concept);
    }

    /**
     * Returns whether the element represents a class/interface known to the current language
     * or to the current compilation round root set.
     */
    public boolean isKnownClass(Element element) {
        if (!(element.getKind().isClass() || element.getKind().isInterface())) {
            return false;
        }

        TypeElement typeElement = (TypeElement) element;
        if (language.getConcept(typeElement.getQualifiedName().toString()) != null) {
            return true;
        }
        if (language.getConcept(normalizedName(typeElement)) != null) {
            return true;
        }

        for (Element elem : rootElements) {
            if (elem.getKind().isClass() || elem.getKind().isInterface()) {
                TypeElement rootTypeElement = (TypeElement) elem;
                if (typeUtils.isSameType(rootTypeElement.asType(), typeElement.asType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizedName(TypeElement typeElement) {
        String name = typeElement.getQualifiedName().toString();
        if (language.getName() != null && !language.getName().isEmpty() && name.startsWith(language.getName() + ".")) {
            return name.substring(language.getName().length() + 1); // +1 because of dot after package name '.'
        }
        return name;
    }

    /**
     * Returns true if superElement is the direct super class of the element.
     * Otherwise, returns false.
     *
     * @param superElement supertype
     * @param element element
     * @return true if superElement is the direct super class of the element.
     */
    private boolean isDirectSubtype(TypeElement superElement, Element element) {
        Exclude excludeAnnotation = element.getAnnotation(Exclude.class);
        int excludeAnnotationsLength = 0;
        try {
            if (excludeAnnotation != null) {
                excludeAnnotation.value();
            }
        } catch (MirroredTypesException e) {
            excludeAnnotationsLength = e.getTypeMirrors().size();
        }

        if (excludeAnnotation == null || excludeAnnotationsLength > 0) {
            if (element.getKind().isClass() || element.getKind().isInterface()) {
                TypeMirror superType = superElement.asType();
                TypeElement typeElement = (TypeElement) element;
                // Test superclass.
                if (typeUtils.isSameType(typeElement.getSuperclass(), superType)) {
                    return true;
                }
                // Test interfaces.
                for (TypeMirror type : typeElement.getInterfaces()) {
                    if (typeUtils.isSameType(type, superType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
