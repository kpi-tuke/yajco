package yajco.parser.beaver;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import beaver.Symbol;

public class SymbolLinkedHashSetImpl<T> extends Symbol implements Set<T> {
    private final LinkedHashSet<T> set;

    public SymbolLinkedHashSetImpl() {
        set = new LinkedHashSet<T>();
    }

    public SymbolLinkedHashSetImpl(Collection<? extends T> c) {
        set = new LinkedHashSet<T>(c);
    }

    public SymbolLinkedHashSetImpl(int initialCapacity) {
        set = new LinkedHashSet<T>(initialCapacity);
    }

    public SymbolLinkedHashSetImpl(int initialCapacity, float loadFactor) {
        set = new LinkedHashSet<T>(initialCapacity, loadFactor);
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean add(T t) {
        boolean successful = set.add(t);
        if (successful) {
            return true;
        }
        throw new RuntimeException("Parse error. Found not unique object: " + t);
    }

    @Override
    public boolean remove(Object o) {
        return set.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return set.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return set.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return set.removeAll(c);
    }

    @Override
    public void clear() {
        set.clear();
    }
}
