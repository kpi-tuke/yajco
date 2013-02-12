package yajco.visitor;

import javax.lang.model.util.*;
import javax.lang.model.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import yajco.model.Language;

public abstract class Visitor<P> {
    private ElementVisitor ev;

    private AbstractElementVisitor6 aev;

    private ElementScanner6 es;

    private Element element;

    private TypeElement te;

    private SimpleElementVisitor6 sev;

//    public void visitProgram(Program program, P p) {
//        visitExpression(expression, p);
//        visitConstantsInProgram(constants, p);
//    }
//
//    public void visitConstants(Constant[] constants, R p) {
//
//    }
//
//    public void visitConstantsInProgram(Constant[] constants, R p) {
//        for(Constant constant : constants) {
//            System.out.println(",");
//            visitConstant(constant, p);
//        }
//    }
//
//    private int zhodnotProgram(Program program) {
//        if(program.getExression() != null) {
//            moznost1 ++;
//            moznost2++;
//        }
//        if(program.getConstants() != null) {
//            moznost2++;
//        }
//    }
//
//    public void visitConstantsInProgramIne(Constant[] constants, R p) {
//        for(Constant constant : constants) {
//            System.out.println(":");
//            visitConstant(constant, p);
//        }
//    }
//
//    public void visitConstant(Constant constant, R p) {
//
//    }
}
