package yajco.xtext.semantics;

import java.io.PrintStream;

public interface CodeRunner {
    void run(PrintStream stream, String code);
}
