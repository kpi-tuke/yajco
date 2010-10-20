package yajco;

import java.io.Serializable;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.Pointcut;
import tuke.pargen.ReferenceResolver;

@Aspect
public class AspectTest {

    private final static String PARSER = "yajco.parser..*";

    @Pointcut("initialization(new(*,@tuke.pargen.annotation.reference.References (*),..)) && args(*,s,..) && within(yajco.model..*) && target(o)")
    public void myAspectTestPointcut(Object o, String s) {
    }

    @Pointcut("call(yajco.model..*.new(..)) && within("+PARSER+")")
    public void myAspectParserPointcut() {
    }

    @After("myAspectTestPointcut(o,s)")
    public void log(Object o, String s){
        System.out.println("-----------------------******************** ASPECTJ "+o+" | "+s+" *******************------------------");
    }

//    @Before("myAspectParserPointcut()")
//    public void test(){
//        System.out.println("=======================******************** AJ in Parser *******************==================");
//    }

//    @DeclareParents("yajco.model..*")
//        Serializable implementedInterface;

}
