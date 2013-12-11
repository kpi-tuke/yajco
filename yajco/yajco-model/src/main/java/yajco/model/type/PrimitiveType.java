package yajco.model.type;

import yajco.annotation.Exclude;

public class PrimitiveType extends Type {

    private PrimitiveTypeConst primitiveTypeConst;

    public PrimitiveType(PrimitiveTypeConst primitiveTypeConst) {
        super(null);
        this.primitiveTypeConst = primitiveTypeConst;
    }

    @Exclude
    public PrimitiveType(PrimitiveTypeConst primitiveTypeConst, Object sourceElement) {
        super(sourceElement);
        this.primitiveTypeConst = primitiveTypeConst;
    }

    //needed for XML binding
    @Exclude
    private PrimitiveType() {
        super(null);
    }

    public PrimitiveTypeConst getPrimitiveTypeConst() {
        return primitiveTypeConst;
    }

    public static PrimitiveType booleanInstance() {
        return new PrimitiveType(PrimitiveTypeConst.BOOLEAN);
    }

    public static PrimitiveType integerInstance() {
        return new PrimitiveType(PrimitiveTypeConst.INTEGER);
    }

    public static PrimitiveType realInstance() {
        return new PrimitiveType(PrimitiveTypeConst.REAL);
    }

    public static PrimitiveType stringInstance() {
        return new PrimitiveType(PrimitiveTypeConst.STRING);
    }
}
