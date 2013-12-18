package yajco.model;

import java.io.Serializable;

public abstract class YajcoModelElement implements Serializable{

    private final Object sourceElement;

    public YajcoModelElement(Object sourceElement) {
        this.sourceElement = sourceElement;
    }

    public Object getSourceElement() {
        return sourceElement;
    }
}
