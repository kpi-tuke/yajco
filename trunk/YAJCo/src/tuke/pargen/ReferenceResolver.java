package tuke.pargen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.reference.Identifier;
import tuke.pargen.annotation.reference.References;

//Nemalo by to byt staticke mal by to mat parser v sebe
//malo by to fungovat aj na zaklade mena parametra, problem je, ze to je runtime reflexia a tam nemam mena
public class ReferenceResolver {
    /** Singleton. */
    private static ReferenceResolver instance;

    private static final String IDENT_ELEMENT_NAME = "identifier";

    private static final String USER_OBJECT_KEY = "object";

    private Map<Object, Element> xmlElements = new HashMap<Object, Element>();

    /**
     * During the processing is the last node root. At the end root node will become the root node of XML document.
     */
    private Element rootElement;

    /**
     * Document element.
     */
    private Document document;

    /** 
     * XPath support.
     */
    private XPath xpath;

    /** Map from class to field with Identifier annotation. */
    private Map<Class, Field> identifierFields = new HashMap<Class, Field>();

    /** List of already processed classes without Identifier  annotation. 
     * Created just for performance reasons for faster resolving of identifiers fields.
     */
    private Set<Class> classesWithoutIdentifiers = new HashSet<Class>();

    /** List of nodes requiring reference. */
    private List<ReferenceItem> nodesToResolve = new ArrayList<ReferenceItem>();

    /** List of all objects that have been registered int the resolver. */
    private List<Object> registeredObjects = new ArrayList<Object>();

    /** Cache map od post construct methods (with annotation PostConstruct) for classes of registered objects. */
    private Map<Class<?>, List<Method>> postConstructMethods = new HashMap<Class<?>, List<Method>>();

    private ReferenceResolver() {
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            xpath = XPathFactory.newInstance().newXPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ReferenceResolver createInstance() {
        instance = new ReferenceResolver();
        return instance;
    }

    public static ReferenceResolver getInstance() {
        if (instance == null) {
            createInstance();
        }
        return instance;
    }

    /**
     * Registers DOM node for AST node with specified childrens.
     * @param <T>   
     * @param object  AST node
     * @param objects  child AST nodes
     * @return the same object as passed by parameter object
     */
    public <T> T register(T object, Object... objects) {
        registeredObjects.add(object);
        analyzeConstructor(object, objects);
        createXmlNode(object, objects);
        return object;
    }

    /**
     * Resolves references and test uniqueness.
     */
    public void resolveReferences() {
        document.appendChild(rootElement);

        //printDocument();

        //Test uniqueness of identifiers
        testUniqueness();

        //Resolve references
        for (ReferenceItem ri : nodesToResolve) {
            resolveReference(ri);
        }

        //Invoke post construct methods
        invokePostConstructMethods();
    }

    /**
     * Test uniqueness of identifiers nodes.
     * Selects every identifier node and tests its uniqueness.
     */
    private void testUniqueness() {
        try {
            //Create XPath expression for selecting all identifiers node
            String expr = "//" + IDENT_ELEMENT_NAME;
            XPathExpression xexpr = xpath.compile(expr);

            //Select all identifier nodes and test uniqueness
            NodeList list = (NodeList) xexpr.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < list.getLength(); i++) {
                testUniqueness(list.item(i));
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test uniqueness of identifier node.
     * @param node dom node
     */
    private void testUniqueness(Node node) {
        try {
            //Get the parent node of identifier node, get user object, find Identifier annotation
            String ident = node.getTextContent();
            node = node.getParentNode();
            Object object = node.getUserData(USER_OBJECT_KEY);
            Identifier identifier = identifierFields.get(object.getClass()).getAnnotation(Identifier.class);

            //Select ident nodes
            String expr;
            if ("".equals(identifier.unique())) {
                expr = "//" + IDENT_ELEMENT_NAME + "[text()='" + ident + "']";
            } else {
                expr = identifier.unique() + "/" + IDENT_ELEMENT_NAME + "[text()='" + ident + "']";
            }
            XPathExpression xexpr = xpath.compile(expr);

            //Select ident nodes
            NodeList list = (NodeList) xexpr.evaluate(node, XPathConstants.NODESET);
            if (list.getLength() > 1) {
                throw new RuntimeException("More than one ident with name '" + ident + "' of type '" + object.getClass() + "' exist");
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }

    }

    private void createXmlNode(Object object, Object... objects) {
        rootElement = document.createElement(object.getClass().getCanonicalName());
        //System.out.println("Registering " + object + " " + rootElement + " " + Arrays.toString(objects));

        for (Object param : objects) {
            if (param != null) {
                if (param.getClass().isArray()) {
                    int length = Array.getLength(param);
                    for (int i = 0; i < length; i++) {
                        Object item = Array.get(param, i);
                        Element childElement = xmlElements.get(item);
                        if (childElement != null) {
                            rootElement.appendChild(childElement);
                        }
                    }
                } else {
                    Element childElement = xmlElements.get(param);
                    if (childElement != null) {
                        rootElement.appendChild(childElement);
                    }
                }
            }
        }
        rootElement.setUserData(USER_OBJECT_KEY, object, null);
        xmlElements.put(object, rootElement);

        //Find identifier field, add it to the node
        Field field = getIdentifierField(object.getClass());
        if (field != null) {
            try {
                field.setAccessible(true);
                Object idvalue = field.get(object);
                if (field.getType().isArray()) {
                    int length = Array.getLength(idvalue);
                    for (int i = 0; i < length; i++) {
                        Object item = Array.get(idvalue, i);
                        Element identifierElement = document.createElement(IDENT_ELEMENT_NAME);
                        identifierElement.setTextContent(item.toString());
                        rootElement.appendChild(identifierElement);
                    }
                } else {
                    Element identifierElement = document.createElement(IDENT_ELEMENT_NAME);
                    identifierElement.setTextContent(idvalue.toString());
                    rootElement.appendChild(identifierElement);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void resolveReference(ReferenceItem ri) {
        try {
            //System.out.println("Resolving reference: " + ri);

            //Create xpath expression
            String expr;

            //If referencing value is null then there is nothing to reference
            if (ri.referencingValue == null) {
                return;
            }

            if ("".equals(ri.references.path())) {
                expr = "//" + ri.referencedClass.getName() + "[" + IDENT_ELEMENT_NAME + "/text()='" + ri.referencingValue.toString() + "']";
            } else {
                expr = ri.references.path() + "[" + IDENT_ELEMENT_NAME + "/text()='" + ri.referencingValue.toString() + "']";
            }

            //System.out.println("Evaluating XPath: " + expr);
            XPathExpression xexpr = xpath.compile(expr);

            //Select node
            //TODO: toto je zle nemozem prezerat cely dokumnet ale musi to byt lokalne
            //musim najst aktualny uzol            
            Node node = (Node) xexpr.evaluate(xmlElements.get(ri.referencingObject), XPathConstants.NODE);

            //Inject reference
            if (node != null) {
                setValue(ri.referencingField, ri.referencingObject, node.getUserData(USER_OBJECT_KEY));
            } else {
                throw new RuntimeException("Reference '" + ri.referencingValue + "' to object of type '" + ri.referencedClass + "' cannot be resolved ");
            }
        } catch (Exception e) {
            throw new RuntimeException("Reference '" + ri.referencingValue + "' to object of type '" + ri.referencedClass + "' cannot be resolved ", e);
        }
    }

    //TODO: Funguje iba s jednym konstruktorom.
    //mal by som to prechadzat na urovni zdrojovych kodov a nie class lebo nemam mena parametrov
    private void analyzeConstructor(Object object, Object[] objects) {
        Class<?> clazz = object.getClass();
        //TODO: tu sa to nemozem viazat na prvy konstruktor ale na vsetky
        Constructor constructor = findConstructor(clazz, objects);
        Type[] params = constructor.getParameterTypes();
        Annotation[][] allAnnotations = constructor.getParameterAnnotations();
        for (int i = 0; i < params.length; i++) {
            for (Annotation annotation : allAnnotations[i]) {
                if (annotation instanceof References) {
                    References references = (References) annotation;
                    Class referencedClass = references.value();
                    String fielddName = references.field();
                    //TODO: Tu to tiez asi zavisi od pola
                    Object referencingValue = objects[i];
                    Field referencingField = getFieldOfType(clazz, referencedClass, fielddName);
                    ReferenceItem ri = new ReferenceItem(references, referencedClass, object, referencingValue, referencingField);
                    nodesToResolve.add(ri);
                }
            }
        }
    }

    private Constructor findConstructor(Class clazz, Object[] objects) {
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            //System.out.println(constructor + " " + Arrays.toString(constructor.getDeclaredAnnotations()));
            if (constructor.getAnnotation(Exclude.class) != null) {
                continue;
            }

            boolean found = true;
//            System.out.println("******************* "+constructor.toGenericString());
//            System.out.println("*****************++ "+objects.length + " / " + constructor.getParameterTypes().length);
            if (constructor.getParameterTypes().length != objects.length) {
                found = false;
            }
            for (int i = 0; i < constructor.getParameterTypes().length; i++) {
                Class class1 = constructor.getParameterTypes()[i];
                Object object = objects[i];
                
                if (class1.isPrimitive()) {
                    if (class1.isAssignableFrom(int.class)) {
                        class1 = Integer.class;
                    } else if (class1.isAssignableFrom(boolean.class)) {
                        class1 = Boolean.class;
                    } else if (class1.isAssignableFrom(float.class)) {
                        class1 = Float.class;
                    } else if (class1.isAssignableFrom(double.class)) {
                        class1 = Double.class;
                    } else if (class1.isAssignableFrom(byte.class)) {
                        class1 = Byte.class;
                    } else if (class1.isAssignableFrom(short.class)) {
                        class1 = Short.class;
                    } else if (class1.isAssignableFrom(char.class)) {
                        class1 = Character.class;
                    } else if (class1.isAssignableFrom(long.class)) {
                        class1 = Long.class;
                    }
                }

//                System.out.println("*** Class "+i+": "+class1.getName()+" / "+objects[i].getClass().getName());
                
                if (object != null && !class1.isInstance(object)) {
                  found = false;
                  break;
                }
            }
            if (!found) {
                continue;
            }
            //System.out.println("********* IT IS THIS!");
            return constructor;            
        }

        throw new RuntimeException("Suitable constructor does not exist '" + clazz + "' for values " + Arrays.toString(objects));
    }

    private Field getFieldOfType(Class clazz, Class type, String fielddName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().equals(type)) {
                if ("".equals(fielddName) || fielddName.equals(field.getName())) {
                    return field;
                }
            }
        }
        throw new RuntimeException("Referencing field of type '" + type + "' not found in class '" + clazz + "'");
    }

    private Field getIdentifierField(Class clazz) {
        {
            Field field = identifierFields.get(clazz);
            if (field != null) {
                return field;
            }
            if (classesWithoutIdentifiers.contains(clazz)) {
                return null;
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(Identifier.class) != null) {
                identifierFields.put(clazz, field);
                return field;
            }
        }

        classesWithoutIdentifiers.add(clazz);
        return null;
    }

    //TODO: potom upravit nielen priamo na triedu ale aj predkov
    private void setValue(Field field, Object object, Object value) throws Exception {
        Class<?> clazz = field.getDeclaringClass();
        String name = field.getName();
        String methodName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);

        //System.out.println("Setting to class=" + clazz + " field=" + field + ", object=" + object + ", value=" + value);

        //Try to find public setter
        try {
            Method method = clazz.getMethod(methodName, field.getType());
            method.invoke(object, value);
            return;
        } catch (NoSuchMethodException e) {
            //If not found than try another strategy
        }

        //Try to use private setter using accessible
        try {
            Method method = clazz.getDeclaredMethod(methodName, field.getType());
            method.setAccessible(true);
            method.invoke(object, value);
            return;
        } catch (NoSuchMethodException e) {
            //If not found than try another strategy
        }

        //Try use field
        field.setAccessible(true);
        field.set(object, value);
    }

    private void invokePostConstructMethods() {
        for (Object o : registeredObjects) {
            Class<?> clazz = o.getClass();
            List<Method> methods;
            if (postConstructMethods.containsKey(clazz)) {
                methods = postConstructMethods.get(clazz);
            } else {
                methods = findPostConstructMethods(clazz);
                postConstructMethods.put(clazz, methods);
            }

            for (Method method : methods) {
                try {
                    method.invoke(o, new Object[]{});
                } catch (Exception e) {
                    throw new RuntimeException("Cannot invoke method " + method + "on object " + o, e);
                }
            }
        }
    }

    private List<Method> findPostConstructMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<Method>();

        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(PostConstruct.class) != null) {
                methods.add(method);
            }
        }

        return methods;
    }

    private void printDocument() {
        try {
            System.out.println("\n");
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(System.out));
            System.out.println("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Representation of referenced element.
     */
    private static class ReferenceItem {
        private final Class referencedClass;

        private final Object referencingObject;

        private final Object referencingValue;

        private final Field referencingField;

        private final References references;

        public ReferenceItem(References references, Class referencedClass, Object referencingObject, Object referencingValue, Field referencingField) {
            this.references = references;
            this.referencedClass = referencedClass;
            this.referencingObject = referencingObject;
            this.referencingValue = referencingValue;
            this.referencingField = referencingField;
        }

        @Override
        public String toString() {
            return "referencedClass=" + referencedClass + ", referencingObject=" + referencingObject + ", referencingValue=" + referencingValue +
                    ", referencingField=" + referencingField + ", References=" + references;
        }
    }
}
