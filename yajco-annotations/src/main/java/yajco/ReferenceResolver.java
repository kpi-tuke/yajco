package yajco;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import yajco.annotation.Exclude;
import yajco.annotation.reference.Identifier;
import yajco.annotation.reference.References;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

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
    private final Map<Class<?>, Field> identifierFields = new HashMap<>();

    /** List of already processed classes without Identifier  annotation.
     * Created just for performance reasons for faster resolving of identifiers fields.
     */
    private final Set<Class<?>> classesWithoutIdentifiers = new HashSet<>();

    /** List of nodes requiring reference. */
    private List<ReferenceItem> nodesToResolve = new ArrayList<ReferenceItem>();

    /** List of all objects that have been registered in the resolver. */
    private List<Object> registeredObjects = new ArrayList<Object>();

    /** List of all objects that their post construct methods have been executed,
     * not participating in active reference resolving, only they can be injected
     * to other objects if contains @Identifier field */
    private List<Object> postConstructExecutedObjects = new ArrayList<Object>();

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
            return createInstance();
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
        return register(object, null, objects);
    }

    /**
     * Registers DOM node for AST node with specified childrens and factory method name.
     * @param <T>
     * @param object  AST node
     * @param methodName name of called factory method
     * @param objects  child AST nodes
     * @return the same object as passed by parameter object
     */
    public <T> T register(T object, String methodName, Object... objects) {
        registeredObjects.add(object);
        analyzeConstructor(object, methodName, objects);
        createXmlNode(object, objects);
        resolveReferences();
        return object;
    }

    /**
     * Check if all registered objects were properly resolved.
     * @return true if for every object was found reference resolusion, false otherwise.
     */
    public boolean isAllResolved() {
        return nodesToResolve.isEmpty();
    }

    public List<Object> getUnresolvedObjects() {
        List<Object> unresolvedObjects = new ArrayList<Object>();
        for (ReferenceItem referenceItem : nodesToResolve) {
            unresolvedObjects.add(referenceItem.referencingObject);
        }
        return unresolvedObjects;
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
        List<ReferenceItem> resolvedNodes = new ArrayList<ReferenceItem>();
        for (ReferenceItem ri : nodesToResolve) {
            if (resolveReference(ri)) {
                resolvedNodes.add(ri);
            }
        }
        nodesToResolve.removeAll(resolvedNodes);

        //Invoke post construct methods
        invokePostConstructMethods();

        //remove child for possible next iteration
        document.removeChild(rootElement);
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
//        System.out.println("Registering " + object + " " + rootElement + " " + Arrays.toString(objects));

        for (Object param : objects) {
            if (param != null) {
                // If param is Optional get inner Object.
                if (param instanceof Optional && ((Optional) param).isPresent()) {
                    param = ((Optional) param).get();
                }
                if (param instanceof Collection) {
                    param = ((Collection)param).toArray();
                }
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

    private boolean resolveReference(ReferenceItem ri) {
        try {
            //System.out.println("Resolving reference: " + ri);

            //Create xpath expression
            String expr;

            //If referencing value is null then there is nothing to reference
            if (ri.referencingValue == null) {
                return true;
            }

            if ("".equals(ri.references.path())) {
                expr = "//" + ri.referencedClass.getName() + "[" + IDENT_ELEMENT_NAME + "/text()='" + ri.referencingValue.toString() + "']";
            } else {
                if (ri.references.path().contains("##cmp##")) {
                    expr = ri.references.path().replace("##cmp##", IDENT_ELEMENT_NAME + "/text()='" + ri.referencingValue.toString()+"'");
                } else {
                    expr = ri.references.path() + "[" + IDENT_ELEMENT_NAME + "/text()='" + ri.referencingValue.toString() + "']";
                }
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
                //all is good
                return true;
            } else {
                return false;
                //throw new RuntimeException("Reference '" + ri.referencingValue + "' to object of type '" + ri.referencedClass + "' cannot be resolved ");
            }
        } catch (Exception e) {
            throw new RuntimeException("Reference '" + ri.referencingValue + "' to object of type '" + ri.referencedClass + "' cannot be resolved ", e);
        }
    }

    private void analyzeConstructor(Object object, String methodName, Object[] objects) {
        Class<?> clazz = object.getClass();
        AccessibleObject accesibleObject = findConstructor(clazz, methodName, objects);
        Type[] params;
        Annotation[][] allAnnotations;

        if (accesibleObject instanceof Constructor) {
            params = ((Constructor)accesibleObject).getParameterTypes();
            allAnnotations = ((Constructor)accesibleObject).getParameterAnnotations();
        } else if (accesibleObject instanceof Method) {
            params = ((Method)accesibleObject).getParameterTypes();
            allAnnotations = ((Method)accesibleObject).getParameterAnnotations();
        } else {
            throw new RuntimeException("Not known type of constructor/method");
        }

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

    private AccessibleObject findConstructor(Class clazz, String methodName, Object[] objects) {
        List<AccessibleObject> list = new ArrayList<AccessibleObject>();
        if (methodName == null || methodName.isEmpty()) {
            list.addAll(Arrays.asList(clazz.getDeclaredConstructors()));
        } else {
            list.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        }
        for (AccessibleObject ao : list) {
            Class<?>[] parameterTypes = new Class<?>[0];
            if (ao instanceof Constructor) {
                parameterTypes = ((Constructor)ao).getParameterTypes();
            } else if (ao instanceof Method) {
                Method method = (Method)ao;
                if (!method.getName().equals(methodName)) {
                    // Method is not of specified name, do not analyze it
                    continue;
                }
                parameterTypes = method.getParameterTypes();
            }
            //System.out.println(constructor + " " + Arrays.toString(constructor.getDeclaredAnnotations()));
            if (ao.getAnnotation(Exclude.class) != null) {
                continue;
            }
            if (parameterTypes.length != objects.length) {
                //found = false;
                continue;
            }
            boolean found = true;
//            System.out.println("******************* "+constructor.toGenericString());
//            System.out.println("*****************++ "+objects.length + " / " + constructor.getParameterTypes().length);

            for (int i = 0; i < parameterTypes.length; i++) {
                Class class1 = parameterTypes[i];
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
            //System.out.println("********* THIS IS IT!");
            return ao;
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

    private <T> Field getIdentifierField(Class<T> clazz) {
        return getIdentifierField(clazz, clazz);
    }

    /**
     * You should call {@link #getIdentifierField(Class)} to start this search (with {@code clazz == requester}).
     * <p>
     * Search for the field annotated with {@link Identifier} in {@code clazz} and recursively in its parents (up to Object).
     * All of the classes including and between requester and clazz will be cached in {@link #identifierFields}.
     *
     * @param clazz     the current class to check for the identifier field
     * @param requester the original subclass whose identifier field we are searching
     * @param <T>       type of the class this recursive search started with
     * @return identifier field of requester (declared in {@code requester} or one of its superclasses),
     * or null if requester does not have an identifier field
     */
    private <T> Field getIdentifierField(Class<? super T> clazz, Class<T> requester) {
        if (clazz == null) { // == Object.class.getSuperclass()
            return null;
        }
        {
            Field field = identifierFields.get(clazz);
            if (field != null) {
                return cacheIdentifierField(field, requester, clazz);
            }
            if (classesWithoutIdentifiers.contains(clazz)) {
                return null;
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(Identifier.class) != null) {
                return cacheIdentifierField(field, requester, clazz);
            }
        }

        classesWithoutIdentifiers.add(clazz);
        return getIdentifierField(clazz.getSuperclass(), requester);
    }

    /**
     * @return input field
     */
    private <T> Field cacheIdentifierField(Field field, Class<T> from, Class<? super T> to) {
        // from <= clsToCache <= to
        for (Class<? super T> clsToCache = from; clsToCache != null && to.isAssignableFrom(clsToCache); clsToCache = clsToCache.getSuperclass()) {
            identifierFields.put(clsToCache, field);
        }
        return field;
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
        List<Object> toPostConstruct = new ArrayList<Object>(registeredObjects);
        toPostConstruct.removeAll(postConstructExecutedObjects);
        for (ReferenceItem ri : nodesToResolve) {
            toPostConstruct.remove(ri.referencingObject);
        }

        for (Object o : toPostConstruct) {
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
                    postConstructExecutedObjects.add(o);
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
