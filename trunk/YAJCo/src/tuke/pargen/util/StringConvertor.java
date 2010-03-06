package tuke.pargen.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class StringConvertor {
    public static final String INPUT_FILE = "c:/Projects/AnnotationDesignator/src/tuke/annotation/processor/target-lang.jj";;
    public static final String OUTPUT_FILE = "c:/Projects/AnnotationDesignator/src/tuke/annotation/processor/grammar.jj";;
    
    public static void main(String[] args) throws IOException {
        String content = readFileAsString(INPUT_FILE);
        //content = content.replace("\\", "\\\\");
        content = content.replace("\"", "\\\"");
        //content = content.replace("\n", "\\n\" + \n\"");
        
        content = content.replace("| <", "        @TokenDef(name = \"");
        content = content.replace(": ", "\", regexp = \"");
        content = content.replace(" >", "\"),");
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE));
        writer.write(content);
        writer.close();
        
        System.out.println(content);
    }
    
    public static String readFileAsString(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        
        StringBuilder sb = new StringBuilder();
        String s;
        while((s = reader.readLine()) != null) {
            sb.append(s + "\n");
        }
        
        reader.close();
        
        return sb.toString();
    }    
}
