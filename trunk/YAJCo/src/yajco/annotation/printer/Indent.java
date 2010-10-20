package yajco.annotation.printer;

import yajco.annotation.processor.MapsTo;

@MapsTo(yajco.model.pattern.impl.printer.Indent.class)
public @interface Indent {
    int level() default 1;
}
