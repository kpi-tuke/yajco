package yajco.xtext.commons.model;

import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.Property;
import yajco.model.pattern.impl.Associativity;
import yajco.model.pattern.impl.Parentheses;

import java.util.ArrayList;
import java.util.List;

import static yajco.xtext.commons.model.RuleUtils.makeCamelCaseName;

public class Rule {
    protected SuperRule superRule;
    protected String name;
    protected String originalName;
    protected boolean isUsed = false;
    protected Rule next;
    protected RuleType type;
    protected List<Notation> cs;
    protected List<Property> as;
    protected String parent;
    protected Associativity associativity;
    protected int priority;
    protected Parentheses parentheses;
    protected String leftParenthesis;
    protected String rightParenthesis;
    protected List<Node<NotationPart>> optimizedNotations;

    public SuperRule getSuperRule() {
        return superRule;
    }

    public void setSuperRule(SuperRule superRule) {
        this.superRule = superRule;
    }

    public void setRightParenthesis(String rightParenthesis) {
        this.rightParenthesis = rightParenthesis;
    }

    public void setLeftParenthesis(String leftParenthesis) {
        this.leftParenthesis = leftParenthesis;
    }

    public List<Node<NotationPart>> getOptimizedNotations() {
        return optimizedNotations;
    }

    public String getLeftParenthesis() {
        return leftParenthesis;
    }

    public String getRightParenthesis() {
        return rightParenthesis;
    }


    public List<Notation> getCs() {
        return cs;
    }

    public void setCs(List<Notation> cs) {
        this.cs = cs;
        this.optimizedNotations = this.optimizeNotations(cs);
    }

    public List<Property> getAs() {
        return as;
    }

    public void setAs(List<Property> as) {
        this.as = as;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = makeCamelCaseName(parent);
    }

    public Associativity getAssociativity() {
        return associativity;
    }

    public void setAssociativity(Associativity associativity) {
        this.associativity = associativity;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Parentheses getParentheses() {
        return parentheses;
    }

    public void setParentheses(Parentheses parentheses) {
        this.parentheses = parentheses;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.originalName = name;
        this.name = makeCamelCaseName(name);
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public Rule getNext() {
        return next;
    }

    public void setNext(Rule next) {
        this.next = next;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    private List<Node<NotationPart>> optimizeNotations(List<Notation> notations) {
        List<Node<NotationPart>> branches = new ArrayList<>();
        if (notations == null || notations.isEmpty()) {
            return null;
        }

        for (Notation notation : notations) {
            Node<NotationPart> branch = null;
            if (notation == null || notation.getParts().isEmpty()) {
                Node<NotationPart> node = new Node<>(null, 0);
                branches.add(node);
                continue;
            } else if (notation.getParts() != null && !notation.getParts().isEmpty()) {
                branch = findNodeBranch(branches, notation.getParts().get(0));
            }

            if (branch == null) {
                createNewBranchFromNotation(branches,notation);
            } else {
                updateBranchFromNotation(branch, notation);
            }
        }
        return branches;
    }

    private void createNewBranchFromNotation(List<Node<NotationPart>> branches, Notation notation){
        Node<NotationPart> prevNode = null;
        Node<NotationPart> parentNode = null;
        for (NotationPart part : notation.getParts()) {
            int level = notation.getParts().indexOf(part);
            Node<NotationPart> node = new Node<>(part, level);
            if (prevNode != null) {
                node.setParent(prevNode);
                prevNode = node;
            } else {
                parentNode = node;
                prevNode = node;
            }
        }
        branches.add(parentNode);
    }

    private void updateBranchFromNotation(Node<NotationPart> branch, Notation notation){
        Node<NotationPart> prevNode = null;
        for (NotationPart part : notation.getParts()) {
            int level = notation.getParts().indexOf(part);
            if (notation.getParts().get(0) == part) {
                prevNode = branch;
            } else {
                Node<NotationPart> current = getObjectFromNextLevel(prevNode, part);
                if (current != null) {
                    prevNode = current;
                    if (prevNode.getChildren().isEmpty()) {
                        Node<NotationPart> newNode = new Node<>(null, prevNode.getLevel() + 1);
                        newNode.setParent(prevNode);
                    }
                } else {
                    current = new Node<>(part, level);
                    current.setParent(prevNode);
                    prevNode = current;
                }
            }
        }
        if (prevNode != null && !prevNode.getChildren().isEmpty()) {
            Node<NotationPart> newNode = new Node<>(null, prevNode.getChildren().get(0).getLevel());
            newNode.setParent(prevNode);
        }
    }

    private Node<NotationPart> findNodeBranch(List<Node<NotationPart>> branches, NotationPart part) {
        if (branches != null && part != null) {
            return branches.stream().filter(node -> node != null && part.equals(node.getData())).findFirst().orElse(null);
        }
        return null;
    }

    private Node<NotationPart> getObjectFromNextLevel(Node<NotationPart> node, NotationPart part) {
        if (node != null && node.getChildren() != null && part != null)
            return node.getChildren().stream().filter(node1 -> node1 != null && part.equals(node1.getData()))
                    .findFirst().orElse(null);
        return null;
    }
}
