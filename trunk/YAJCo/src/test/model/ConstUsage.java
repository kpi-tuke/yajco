package test.model;

import tuke.pargen.annotation.*;
import tuke.pargen.annotation.reference.*;

import yajco.annotation.printer.*;


public class ConstUsage extends Factor {
    private Constant constant;

    public ConstUsage(@References(value = Constant.class)
    String name) {
    }

    public Constant getConstant() {
        return constant;
    }

    public void setConstant(Constant constant) {
        this.constant = constant;
    }
}
