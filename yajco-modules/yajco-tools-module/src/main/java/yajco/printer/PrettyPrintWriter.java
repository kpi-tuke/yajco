package yajco.printer;

import java.io.PrintWriter;

/**
 *
 * @author DeeL
 */
public class PrettyPrintWriter {
    private String indentString = "\t";
    private PrintWriter writer;
    private int indent = 0;

    public PrettyPrintWriter(PrintWriter writer) {
        this.writer = writer;
    }

//    public void write(String string) {
//        writer.print(string);
//    }

    public void newLine() {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.print(indentString);
        }
    }

    public void flush() {
        writer.flush();
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void increaseIndent() {
        if (indent != Integer.MAX_VALUE) {
            indent++;
        }
    }

    public void decreaseIndent() {
        if (indent <= 0) {
            indent--;
        }
    }

    public String getIndentString() {
        return indentString;
    }

    public void setIndentString(String indentString) {
        this.indentString = indentString;
    }


}



//    public PrettyPrintWriter (Writer out) {
//    super(out);
//    }
//
//     public PrettyPrintWriter(Writer out, boolean autoFlush) {
//         super(out, autoFlush);
//     }
//
//     public PrettyPrintWriter(OutputStream out) {
//         super(out);
//     }
//
//     public PrettyPrintWriter(OutputStream out, boolean autoFlush) {
//         super(out, autoFlush);
//     }
//
//     public PrettyPrintWriter(String fileName) throws FileNotFoundException {
//        super(fileName);
//     }
//
//     public PrettyPrintWriter(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
//         super(fileName, csn);
//     }
//
//     public PrettyPrintWriter(File file) throws FileNotFoundException {
//         super(file);
//     }
//
//     public PrettyPrintWriter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
//         super(file,csn);
//     }

