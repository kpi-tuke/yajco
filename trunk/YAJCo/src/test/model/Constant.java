package test.model;

import tuke.pargen.annotation.*;
import tuke.pargen.annotation.reference.*;

import yajco.annotation.printer.*;


public class Constant {
    @Identifier
    private String name;
    private Number number;

    public Constant(String name, @Before("=")
    Number number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number getNumber() {
        return number;
    }

    public void setNumber(Number number) {
        this.number = number;
    }
}
