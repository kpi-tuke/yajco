package test.model;

import tuke.pargen.annotation.*;
import tuke.pargen.annotation.reference.*;

import yajco.annotation.printer.*;


public class Number extends Factor {
    private int value;

    public Number(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
