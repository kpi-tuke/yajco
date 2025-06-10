package yajco.model.utilities;

import yajco.model.Concept;
import yajco.model.Property;
import yajco.model.type.Type;

import java.io.Serializable;
import java.util.Objects;

/**
 * A {@link Property} which is possible to be checked for equality with another property,
 * even though they need not be identical (p1 == p2). Useful as a HashMap key.
 */
public class PropertyInConcept implements Serializable {
    private static final long     serialVersionUID = 4378498045837368587L;
    private final        Property property;
    private final        Concept  concept;

    /**
     * @param property property to wrap
     * @param concept  concept in which property is defined
     * @throws NullPointerException     if property or concept is null
     * @throws IllegalArgumentException if property is not defined directly in concept
     */
    public PropertyInConcept(Property property, Concept concept) {
        this.property = Objects.requireNonNull(property);
        this.concept = Objects.requireNonNull(concept);
        if (!concept.getAbstractSyntax().contains(property)) {
            throw new IllegalArgumentException("Property " + property.getName() + " is not defined in " + concept.getName());
        }
    }

    public Property getProperty() {
        return property;
    }

    public Concept getConcept() {
        return concept;
    }

    /**
     * <p>These must be equal:</p>
     * <ol>
     *  <li>{@link Concept}</li>
     *  <li>{@link Property#getName()}</li>
     *  <li>class of {@link Property#getType()}</li>
     * </ol>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyInConcept)) return false;

        final PropertyInConcept that = (PropertyInConcept) o;

        if (!Objects.equals(concept, that.concept)) return false;
        if (!Objects.equals(property.getName(), that.property.getName())) return false;

        final Type type = property.getType();
        final Type thatType = that.property.getType();
        if (type == null || thatType == null) {
            return type == thatType;
        }
        return Objects.equals(type.getClass(), thatType.getClass());
    }

    /**
     * <p>Depends on:</p>
     * <ol>
     *  <li>{@link Concept}</li>
     *  <li>{@link Property#getName()}</li>
     *  <li>class of {@link Property#getType()}</li>
     * </ol>
     */
    @Override
    public int hashCode() {
        final Type type = property.getType();
        return Objects.hash(-455370592, concept, property.getName(), (type == null) ? 0 : type.getClass());
    }
}
