package yajco.model.pattern.impl;

// Copy of enum from project yajco-model to resolve problem with dependency
// user don't need to depend on yajco-model to use annotations
// yajco-model needs this implementation also for generation of bootstrapped yajco parser
public enum Associativity {
    LEFT, RIGHT, NONE, AUTO
}
