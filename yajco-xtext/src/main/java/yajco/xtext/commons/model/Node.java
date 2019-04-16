package yajco.xtext.commons.model;

import java.util.ArrayList;
import java.util.List;


public class Node<T>{
    private T data;
    private List<Node<T>> children = new ArrayList<>();
    private Node<T> parent = null;
    private int level;

    public Node(T data, int level) {
        this.data = data;
        this.level = level;
    }

    public void addChild(Node<T> child) {
        this.children.add(child);
    }

    public void addChild(T data, int level) {
        Node<T> newChild = new Node<>(data, level);
        this.addChild(newChild);
    }

    public void addChildren(List<Node<T>> children) {
        for(Node<T> t : children) {
            t.setParent(this);
        }
        this.children.addAll(children);
    }

    public List<Node<T>> getChildren() {
        return children;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setParent(Node<T> parent) {
        this.parent = parent;
        parent.addChild(this);
    }

    public Node<T> getParent() {
        return parent;
    }

    public int getLevel(){
        return this.level;
    }

    public void setLevel(int level){
        this.level = level;
    }

}