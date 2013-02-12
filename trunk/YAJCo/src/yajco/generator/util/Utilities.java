package yajco.generator.util;

import de.hunsicker.jalopy.Jalopy;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import yajco.generator.GeneratorException;
import yajco.model.BindingNotationPart;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.TokenPart;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.PatternSupport;
import yajco.model.pattern.PropertyPattern;
import yajco.model.pattern.impl.Identifier;
import yajco.model.pattern.impl.References;
import yajco.model.type.ArrayType;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.PrimitiveTypeConst;
import yajco.model.type.ReferenceType;
import yajco.model.type.SetType;
import yajco.model.type.Type;

public class Utilities {

	private static String DEFAULT_MAIN_PACKAGE_NAME = "model";
    private static Jalopy jalopy;

	public static String toUpperCaseIdent(String ident) {
		return Character.toUpperCase(ident.charAt(0)) + ident.substring(1);
	}

	public static String toLowerCaseIdent(String ident) {
		return Character.toLowerCase(ident.charAt(0)) + ident.substring(1);
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

	public static <T> List<T> elementAsList(T element) {
		ArrayList<T> list = new ArrayList<T>();
		list.add(element);

		return list;
	}

	public static boolean isList(Object o) {
		return o instanceof List;
	}

	public static String getClassName(Object o) {
		return o.getClass().getCanonicalName();
	}

	public static Class<?> getClassByName(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

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
		}
		return null;
	}

	public static List<Pattern> getAllPatterns(Property property, Concept concept) {
		List<Pattern> patterns = new ArrayList<Pattern>();
		patterns.addAll(getPatterns(property));
		for (Notation notation : concept.getConcreteSyntax()) {
			for (NotationPart notationPart : notation.getParts()) {
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

	public static Object getObjectFromList(List list, String className) {
		try {
			Class clazz = Class.forName(className);
			for (Object object : list) {
				if (clazz.isInstance(object)) {
					return object;
				}
			}
			return null;
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Class name cannot be found", ex);
		}
	}

	public static boolean hasClassInList(List list, String className) {
		if (getObjectFromList(list, className) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isStringType(Type type) {
		if (type instanceof PrimitiveType) {
			if (((PrimitiveType) type).getPrimitiveTypeConst() == PrimitiveTypeConst.STRING) {
				return true;
			}
		}
		return false;
	}

	public static Property getIdentifierProperty(Concept concept) {
		for (Property property : concept.getAbstractSyntax()) {
			if (property.getPattern(Identifier.class) != null) {
				return property;
			}
		}
		return null;
	}

	public static boolean instanceOf(Object o, Class clazz) {
		return clazz.isInstance(o);
	}

	public static boolean instanceOf(Object o, String className) {
		try {
			return instanceOf(o, getClassByName(className));
		} catch (ClassNotFoundException ex) {
			return false;
		}
	}

	public static String getLanguagePackageName(Language language) {
		if (language.getName() != null) {
			return language.getName();
		} else {
			return DEFAULT_MAIN_PACKAGE_NAME;
		}
	}

	public static String getFullConceptClassName(Language language, Concept concept) {
		return getLanguagePackageName(language) + "." + concept.getName();
	}

    public static Map<Concept, String> createConceptUniqueNames(Language language) {
        Map<Concept, String> map = new HashMap<Concept, String>();
        for (Concept concept : language.getConcepts()) {
            String conceptName = concept.getConceptName();
            for (Concept comparingConcept : language.getConcepts()) {
                if (!concept.equals(comparingConcept) && conceptName.equals(comparingConcept.getConceptName())) {
                    conceptName = getFullConceptClassName(language, concept);
                }
            }
            map.put(concept, conceptName);
        }
        return map;
    }

	public static Concept getTopLevelParent(Concept concept) {
		Concept parent = concept;
		while (parent.getParent() != null) {
			parent = parent.getParent();
		}

		return parent;
	}

    public static String getClassName(Map<Concept, String> map, Concept concept) {
        return map.get(concept);
    }

    public static String getMethodPartName(Map<Concept, String> map, Concept concept) {
        String name = map.get(concept);
        if (name.contains(".")) {
            name = concept.getName().replace(".", "_");
        }
        return name;
    }

    public static List<Concept> getConceptsNeededForImport(Map<Concept, String> map) {
        List<Concept> concepts = new ArrayList<Concept>();
        for (Concept concept : map.keySet()) {
            if (!map.get(concept).contains(".")) {
                concepts.add(concept);
            }
        }
        return concepts;
    }

    public static void throwGeneratorException(String message) {
        throw new GeneratorException(message);
    }

    public static void formatCode(File file) {
        try {
            Jalopy j = getJalopy();
            j.setInput(file);
            j.setOutput(file);
            j.format();
        } catch (FileNotFoundException ex) {
            throw new GeneratorException("File " + file.getAbsolutePath() + " is not available", ex);
        }
    }

	public static List<Integer> getValuedNotationList(Concept concept) {
		final int BINDING_NOTATION_PART_VALUE = 20;
		final int TOKEN_NOTATION_PART_VALUE = 1;
		List<Integer> list = new ArrayList<Integer>();
		List<ValuedObject<Notation>> localList = new ArrayList<ValuedObject<Notation>>();
		int temp;
		for (Notation notation : concept.getConcreteSyntax()) {
			temp = 0;
			for (NotationPart notationPart : notation.getParts()) {
				if (notationPart instanceof BindingNotationPart) {
					temp += BINDING_NOTATION_PART_VALUE;
				} else if (notationPart instanceof TokenPart) {
					temp += TOKEN_NOTATION_PART_VALUE;
				}
			}
			localList.add(new ValuedObject<Notation>(notation, temp));
		}
		Collections.sort(localList);
		for (ValuedObject<Notation> valuedObject : localList) {
			list.add(Integer.valueOf(concept.getConcreteSyntax().indexOf(valuedObject.object)));
		}
		return list;
	}

    public static void createDirectories(File file) throws GeneratorException {
        createDirectories(file, true);
    }

    public static void createDirectories(File file, boolean isDirectory) throws GeneratorException {
        if (!isDirectory) {
            file = file.getParentFile();
        }
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new GeneratorException("Cannot create directory: " + file.getAbsolutePath());
            }
        }
    }

    public static String getTypeName(Type type) {
        StringBuilder str = new StringBuilder();
        if (type instanceof ArrayType) {
            str.append(getTypeName(((ArrayType) type).getComponentType()));
            str.append("[]");
        } else if (type instanceof ListType) {
            str.append("List<");
            str.append(getTypeName(((ListType) type).getComponentType()));
            str.append(">");
        } else if (type instanceof SetType) {
            str.append("Set<");
            str.append(getTypeName(((SetType) type).getComponentType()));
            str.append(">");
        } else if (type instanceof PrimitiveType) {
            PrimitiveType primitive = (PrimitiveType) type;
            switch (primitive.getPrimitiveTypeConst()) {
                case BOOLEAN:
                    str.append("boolean");
                    break;
                case INTEGER:
                    str.append("int");
                    break;
                case REAL:
                    str.append("double");
                    break;
                case STRING:
                    str.append("String");
                    break;
            }
        } else if (type instanceof ReferenceType) {
            str.append(((ReferenceType) type).getConcept().getConceptName());
        } else {
            throw new GeneratorException("Not known type: " + type.getClass().getCanonicalName());
        }
        return str.toString();
    }

    private synchronized static Jalopy getJalopy() {
        if (jalopy == null) {
            jalopy = new Jalopy();
        }
        return jalopy;
    }

    //Internal class
	private static class ValuedObject<T> implements Comparable {

		private T object;
		private int value = 0;

		public ValuedObject(T object) {
			this.object = object;
		}

		public ValuedObject(T object, int value) {
			this.object = object;
			this.value = value;
		}

		public int compareTo(Object o) {
			if (o instanceof ValuedObject) {
				return ((ValuedObject) o).getValue() - this.getValue();
			} else {
				return 0;
			}
		}

		/**
		 * @return the object
		 */
		public T getObject() {
			return object;
		}

		/**
		 * @param object the object to set
		 */
		public void setObject(T object) {
			this.object = object;
		}

		/**
		 * @return the value
		 */
		public int getValue() {
			return value;
		}

		/**
		 * @param value the value to set
		 */
		public void setValue(int value) {
			this.value = value;
		}
	}
}
