package yajco.model.utilities;

import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.OptionalPart;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.PatternSupport;
import yajco.model.pattern.PropertyPattern;
import yajco.model.pattern.impl.Identifier;
import yajco.model.pattern.impl.References;
import yajco.model.type.PrimitiveType;
import yajco.model.type.PrimitiveTypeConst;
import yajco.model.type.ReferenceType;
import yajco.model.type.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utilities {

    private static String DEFAULT_MAIN_PACKAGE_NAME = "model";

    public static <T> List<T> asList(T[] a) {
        if (a == null) {
            return new ArrayList<T>();
        }
        return new ArrayList<T>(Arrays.asList(a));
    }

    public static String getLanguagePackageName(Language language) {
        if (language.getName() != null) {
            return language.getName();
        } else {
            return DEFAULT_MAIN_PACKAGE_NAME;
        }
    }

// <editor-fold defaultstate="collapsed" desc="Utility methods needed for grammar module">
// ----------------------------------------------
// ---------- NEEDED for GRAMMAR MODULE ---------
// ----------------------------------------------
    public static List<Concept> getDirectDescendantConcepts(Concept parent, Language language) {
        if (parent == null || language == null) {
            //throw new NullPointerException("Parameters not allowed to be null.");
            return null;
        }
        List<Concept> list = new ArrayList<Concept>();
        for (Concept concept : language.getConcepts()) {
            if (parent.equals(concept.getParent())) {
                list.add(concept);
            }
        }
        return list;
    }

    public static String getFullConceptClassName(Language language, Concept concept) {
        return getLanguagePackageName(language) + "." + concept.getName();
    }

    public static Concept getTopLevelParent(Concept concept) {
        Concept parent = concept;
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }

        return parent;
    }
// ----------------------------------------------    
// ---END---- NEEDED for GRAMMAR MODULE ---END---
// ----------------------------------------------    
// </editor-fold>

    public static Property getPropertyFromNotationPart(NotationPart notationPart, Concept concept) {
        if (notationPart instanceof PropertyReferencePart) {
            PropertyReferencePart propertyReferencePart = (PropertyReferencePart) notationPart;
            return propertyReferencePart.getProperty();
        } else if (notationPart instanceof LocalVariablePart) {
            LocalVariablePart localVariablePart = (LocalVariablePart) notationPart;
            References references = (References) localVariablePart.getPattern(References.class);
            if (references != null) {
                if (references.getProperty() != null) {
                    return references.getProperty();
                } else {
                    Property matchedProperty = null;
                    for (Property property : concept.getAbstractSyntax()) {
                        Type type = property.getType();
                        if (type instanceof ReferenceType) {
                            ReferenceType referenceType = (ReferenceType) type;
                            if (referenceType.getConcept().equals(references.getConcept())) {
                                if (matchedProperty == null || localVariablePart.getName().equals(property.getName())) {
                                    matchedProperty = property;
                                }
                            }
                        }
                    }
                    return matchedProperty;
                }
            } else {
                throw new RuntimeException("Cannot find references.");
            }
        } else if (notationPart instanceof OptionalPart) {
            for (NotationPart innerPart : ((OptionalPart) notationPart).getParts()) {
                final Property property = getPropertyFromNotationPart(innerPart, concept);
                // essentially we are skipping NotationParts without a corresponding Property (i.e. TokenParts)
                if (property != null) {
                    return property;
                }
            }
        }
        return null;
    }

    public static List<Pattern> getAllPatterns(Property property, Concept concept) {
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.addAll(getPatterns(property));
        for (Notation notation : concept.getConcreteSyntax()) {
            for (NotationPart notationPart : notation.getParts()) {
                if (notationPart instanceof OptionalPart) {
                    for (NotationPart notationPart1: ((OptionalPart) notationPart).getParts()) {
                        if (property.equals(getPropertyFromNotationPart(notationPart1, concept))) {
                            patterns.addAll(getPatterns(notationPart1));
                            break;
                        }
                    }
                }
                if (property.equals(getPropertyFromNotationPart(notationPart, concept))) {
                    patterns.addAll(getPatterns(notationPart));
                    break;
                }
            }
        }
        return patterns;
    }

    public static List<NotationPartPattern> getPatterns(NotationPart notationPart) {
        List<NotationPartPattern> patterns = new ArrayList<NotationPartPattern>();
        if (notationPart instanceof PatternSupport) {
            PatternSupport<NotationPartPattern> patternSupport = (PatternSupport<NotationPartPattern>) notationPart;
            patterns.addAll(patternSupport.getPatterns());
        }
        return patterns;
    }

    public static List<PropertyPattern> getPatterns(Property property) {
        return property.getPatterns();
    }

    public static boolean isStringType(Type type) {
        if (type instanceof PrimitiveType) {
            if (((PrimitiveType) type).getPrimitiveTypeConst() == PrimitiveTypeConst.STRING) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search for the property with the {@link Identifier} pattern in {@code concept} and recursively in its parents.
     *
     * @param concept the concept whose identifier property we are searching
     * @return identifier property of {@code concept} (declared in {@code concept} or one of its super-concepts),
     * or null if concept does not have an identifier field
     */
    public static PropertyInConcept getIdentifierProperty(Concept concept) {
        if (concept == null) {
            return null;
        }

        for (Property property : concept.getAbstractSyntax()) {
            if (property.getPattern(Identifier.class) != null) {
                return new PropertyInConcept(property, concept);
            }
        }
        return getIdentifierProperty(concept.getParent());
    }

    public static String toUpperCaseNotation(String camelNotation) {
        StringBuilder sb = new StringBuilder(camelNotation.length() + 10);
        boolean change = true;
        for (int i = 0; i < camelNotation.length(); i++) {
            char c = camelNotation.charAt(i);
            change = !change && Character.isUpperCase(c);
            if (change) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
            change = Character.isUpperCase(c);
        }
        return sb.toString();
    }
}
