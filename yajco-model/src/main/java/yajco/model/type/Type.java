package yajco.model.type;

import yajco.model.YajcoModelElement;

/**
 * Kazda property alebo variable ma svoj typ, co moze byt
 * - primitivny
 * - reference
 * - kolekcia (component)
 */
public abstract class Type extends YajcoModelElement {

    public Type(Object sourceElement) {
        super(sourceElement);
    }
}
