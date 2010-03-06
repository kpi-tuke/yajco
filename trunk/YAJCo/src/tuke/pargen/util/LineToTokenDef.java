package tuke.pargen.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LineToTokenDef {
    public static final String INPUT_FILE = "c:/a";;
    public static final String OUTPUT_FILE = "c:/out.txt";;
    
    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(INPUT_FILE));

        StringBuilder sb = new StringBuilder();
        String s;
        while((s = reader.readLine()) != null) {
            sb.append(String.format("        @TokenDef(name = \"%s\", regexp = \"\\\"%s\\\"\"),\n", s.replace("-", "_"), s));
        }

        reader.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE));
        writer.write(sb.toString());
        writer.close();
    }
}
