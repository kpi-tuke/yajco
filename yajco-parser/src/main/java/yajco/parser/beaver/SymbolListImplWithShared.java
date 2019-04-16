package yajco.parser.beaver;

import beaver.Symbol;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SymbolListImplWithShared<T> extends Symbol implements List<T>  {

    private final ArrayList<T> list;

    public SymbolListImplWithShared() {
        list = new ArrayList<T>();
    }

    public SymbolListImplWithShared(Collection<? extends T> c) {
        list = new ArrayList<T>(c);
    }

    public SymbolListImplWithShared(int initialCapacity) {
        list = new ArrayList<T>(initialCapacity);
    }

    public List<T> getUpdatedList(String sharedPartName) {
        int index = 0;
        boolean sharedFirst = false;
        Object sharedValue = null;

        for (T item: list) {
            try {
                Object value = item.getClass().getMethod("get" + sharedPartName).invoke(item);
                Class<?> type = item.getClass().getMethod("get" + sharedPartName).getReturnType();

                if (this.list.indexOf(item) == 0 && value != null) {
                    sharedFirst = true;
                }

                if (value != null) {
                    sharedValue = value;
                }

                if (value == null && sharedValue != null && sharedFirst) {
                    item.getClass().getMethod("set" + sharedPartName, type).invoke(item, sharedValue);
                } else if (value != null && !sharedFirst) {
                    for (int i = index; i < this.list.indexOf(item); i++) {
                        this.list.get(i).getClass().getMethod("set" + sharedPartName, type).invoke(this.list.get(i), value);
                    }
                    index = this.list.indexOf(item) + 1;
                    value = null;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(Object o) {
        return list.contains((T) o);
    }

    public Iterator<T> iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    public boolean add(T e) {
        return list.add(e);
    }

    public boolean remove(Object o) {
        return list.remove((T) o);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public boolean addAll(Collection<? extends T> c) {
        return list.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends T> c) {
        return list.addAll(index, c);
    }

    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    public void clear() {
        list.clear();
    }

    public T get(int index) {
        return list.get(index);
    }

    public T set(int index, T element) {
        return list.set(index, element);
    }

    public void add(int index, T element) {
        list.add(index, element);
    }

    public T remove(int index) {
        return list.remove(index);
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return lastIndexOf(o);
    }

    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }

    public List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
}
