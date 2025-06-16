package yajco.generator.util;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import yajco.generator.GeneratorException;
import yajco.model.*;
import yajco.model.type.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Utilities {

    private static Formatter formatter;

    public static String toUpperCaseIdent(String ident) {
        return Character.toUpperCase(ident.charAt(0)) + ident.substring(1);
    }

    public static String toLowerCaseIdent(String ident) {
        return Character.toLowerCase(ident.charAt(0)) + ident.substring(1);
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

    public static void throwGeneratorException(String message) {
        throw new GeneratorException(message);
    }

    public static void formatCode(File file) {
        try {
            Formatter formatter = getFormatter();
            String source = java.nio.file.Files.readString(file.toPath());
            String formatted = formatter.formatSource(source);
            java.nio.file.Files.writeString(file.toPath(), formatted);
        } catch (FileNotFoundException ex) {
            throw new GeneratorException("File " + file.getAbsolutePath() + " is not available", ex);
        } catch (java.io.IOException ex) {
            throw new GeneratorException("Error reading or writing file " + file.getAbsolutePath(), ex);
        } catch (FormatterException ex) {
            throw new GeneratorException("Error formatting file " + file.getAbsolutePath(), ex);
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

    public static List<Concept> getConceptsNeededForImport(Map<Concept, String> map) {
        List<Concept> concepts = new ArrayList<Concept>();
        for (Concept concept : map.keySet()) {
            if (!map.get(concept).contains(".")) {
                concepts.add(concept);
            }
        }
        return concepts;
    }

    public static Map<Concept, String> createConceptUniqueNames(Language language) {
        Map<Concept, String> map = new HashMap<Concept, String>();
        for (Concept concept : language.getConcepts()) {
            String conceptName = concept.getConceptName();
            for (Concept comparingConcept : language.getConcepts()) {
                if (!concept.equals(comparingConcept) && conceptName.equals(comparingConcept.getConceptName())) {
                    conceptName = yajco.model.utilities.Utilities.getFullConceptClassName(language, concept);
                }
            }
            map.put(concept, conceptName);
        }
        return map;
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

    private synchronized static Formatter getFormatter() {
        if (formatter == null) {
            formatter = new Formatter();
        }
        return formatter;
    }

    public static String encodeStringIntoTokenName(String s) {
        StringBuilder sb = new StringBuilder();
        if ("TOKEN".equals(s.toUpperCase())) {
            return "_TOKEN";
        }
        boolean change = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                change = !change && Character.isUpperCase(c);
                if (change) {
                    sb.append('_');
                }
                change = Character.isUpperCase(c);
                sb.append(c);
            } else if (c == '_') {
                sb.append(c);
            } else {
                sb.append('_').append(((int) c));
            }

        }
        return sb.toString().toUpperCase();
    }

    public static String encodeStringToJavaLiteral(String s) {
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return s;
    }

    public static String encodeStringIntoRegex(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (c == '[') {
                sb.append("\\[");
            } else if (c == '^') {
                sb.append("\\^");
            } else {
                sb.append("[" + c + "]");
            }
        }
        return sb.toString();
    }

    public static File createFile(File directory, String packageName, String fileName) {
        String fullFileName = packageName.replace('.', File.separatorChar) + File.separator + fileName;
        File file = new File(directory, fullFileName);
        createDirectories(file, false);
        return file;
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

        @Override
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
