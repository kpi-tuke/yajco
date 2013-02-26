package yajco.model;

public abstract class YajcoModelElement {

    private Object sourceElement;

    public YajcoModelElement(Object sourceElement) {
        this.sourceElement = sourceElement;
    }

    public Object getSourceElement() {
        return sourceElement;
    }
}
