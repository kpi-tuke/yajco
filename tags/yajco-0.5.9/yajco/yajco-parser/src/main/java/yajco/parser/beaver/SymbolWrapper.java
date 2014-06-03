package yajco.parser.beaver;

import beaver.Symbol;

public class SymbolWrapper<T> extends Symbol{
    T wrappedObject;

    public SymbolWrapper(T wrappedObject) {
        this.wrappedObject = wrappedObject;
    }

    public T getWrappedObject() {
        return wrappedObject;
    }

}