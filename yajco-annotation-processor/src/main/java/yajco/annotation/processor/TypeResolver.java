package yajco.annotation.processor;

import yajco.generator.GeneratorException;
import yajco.model.Concept;
import yajco.model.type.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Maps Java types to YAJCo model types.
 */
public class TypeResolver {
    public interface ConceptTypeGateway {
        Concept resolveIfKnown(TypeElement typeElement); // returns null if not model concept
    }

    private final Types typeUtils;
    private final ConceptTypeGateway conceptGateway;

    public TypeResolver(Types typeUtils, ConceptTypeGateway conceptGateway) {
        this.typeUtils = typeUtils;
        this.conceptGateway = conceptGateway;
    }

    /**
     * Finds YAJCo model type of parameter.
     *
     * @param type Element type.
     * @return YAJCo model type of parameter.
     */
    public Type getType(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return new ArrayType(
                getSimpleType(((javax.lang.model.type.ArrayType) type).getComponentType()));
        } else if (isSpecifiedClassType(type, List.class)) {
            return getSpecifiedYajcoComponentType(type, ListType.class);
        } else if (isSpecifiedClassType(type, Set.class)) {
            return getSpecifiedYajcoComponentType(type, SetType.class);
        } else if (isSpecifiedClassType(type, Optional.class)) {
            return getSpecifiedYajcoComponentType(type, OptionalType.class);
        } else {
            return getSimpleType(type);
        }
    }

    /**
     * Finds YAJCo component type of element.
     *
     * @param type Type of language model element.
     * @param yajcoType YAJCo model type.
     * @return YAJCo component type of element.
     */
    private <T extends ComponentType> T getSpecifiedYajcoComponentType(TypeMirror type, Class<T> yajcoType) {
        if (type.getKind() != TypeKind.DECLARED) {
            throw new GeneratorException("Type " + type + " is not class or interface");
        }

        System.out.println("************************ " + type.getKind());

        List<? extends TypeMirror> types = ((DeclaredType) type).getTypeArguments();

        if (types.isEmpty()) {
            throw new GeneratorException("Not specified type for " + type + ", please use generics to specify inner type.");
        } else {
            try {
                Constructor<T> constructor = yajcoType.getConstructor(Type.class);
                if (typeUtils.asElement(type).toString().equals(Optional.class.getName())) {
                    // Component type as Optional. For example Optional<String[]>
                    return constructor.newInstance(getType(types.get(types.size() - 1)));
                }
                return constructor.newInstance(getSimpleType(types.get(types.size() - 1)));
            } catch (NoSuchMethodException ex) {
                throw new GeneratorException("Cannot find constructor for " + yajcoType.getName() + " with only " + Type.class.getName() + " paramater!", ex);
            } catch (Exception ex) {
                throw new GeneratorException("Cannot create new object (" + yajcoType.getName() + ") needed for type " + type, ex);
            }
        }
    }

    private boolean isSpecifiedClassType(TypeMirror type, Class<?> clazz) {
        TypeElement referencedTypeElement = (TypeElement) typeUtils.asElement(type);
        return clazz != null && referencedTypeElement != null
               && referencedTypeElement.getQualifiedName().toString().equals(clazz.getName());
    }

    /**
     * Finds YAJCo model type of argument.
     *
     * @param type Type of language model element.
     * @return YAJCo model type of argument.
     */
    public Type getSimpleType(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            PrimitiveTypeConst primTypeConst = PrimitiveTypeConst.INTEGER;
            switch (type.getKind()) {
                case BOOLEAN:
                    primTypeConst = PrimitiveTypeConst.BOOLEAN;
                    break;
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                    primTypeConst = PrimitiveTypeConst.INTEGER;
                    break;
                case FLOAT:
                case DOUBLE:
                    primTypeConst = PrimitiveTypeConst.REAL;
                    break;
            }
            return new PrimitiveType(primTypeConst, type);
        } else if (type.toString().equals(String.class.getName())) {
            return new PrimitiveType(PrimitiveTypeConst.STRING, type);
        } else if (type.toString().equals(Boolean.class.getName())) {
            return new PrimitiveType(PrimitiveTypeConst.BOOLEAN, type);
        } else if (type.toString().equals(Byte.class.getName())
                   || type.toString().equals(Short.class.getName())
                   || type.toString().equals(Integer.class.getName())
                   || type.toString().equals(Long.class.getName())) {
            return new PrimitiveType(PrimitiveTypeConst.INTEGER, type);
        } else if (type.toString().equals(Float.class.getName()) || type.toString().equals(Double.class.getName())) {
            return new PrimitiveType(PrimitiveTypeConst.REAL, type);
        } else if (type.getKind() == TypeKind.DECLARED) {
            TypeElement referencedTypeElement = (TypeElement) typeUtils.asElement(type);
            System.out.println("getSimpleType(): referencedTypeElement: " + referencedTypeElement);
            Concept c = conceptGateway.resolveIfKnown(referencedTypeElement);
            if (c != null) return new ReferenceType(c, referencedTypeElement);
        }
        throw new GeneratorException("Unsupported simple type " + type + " [" + type.getKind() + "]");
    }
}
