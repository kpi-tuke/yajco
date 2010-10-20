package yajco.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import tuke.pargen.GeneratorException;
import yajco.generator.util.Utilities;
import yajco.model.Language;

public abstract class AbstractFileGenerator implements FilesGenerator {

    public abstract void generate(Language language, Writer writer);

    public abstract String getPackageName();

    public abstract String getFileName();

    public abstract String getClassName();

    public void generate(Language language, File file) {
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            generate(language, writer);
            Utilities.formatCode(file);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write to file " + file.getAbsolutePath(), ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                throw new GeneratorException("Cannot close writer for file " + file.getAbsolutePath(), ex);
            }
        }
    }

    public void generateFiles(Language language, File directory) {
        File file = getFileToWrite(language, directory, getPackageName(), getFileName());
        generate(language, file);
    }

    protected File getFileToWrite(Language language, File directory, String packageName, String fileName) {
        if (!directory.isDirectory()) {
            throw new GeneratorException("Supplied file argument is not directory: " + directory.getAbsolutePath());
        }
        String filePath = Utilities.getLanguagePackageName(language).replace('.', File.separatorChar) + File.separator + packageName + File.separator + fileName;
        File file = new File(directory, filePath);
        Utilities.createDirectories(file, false);
        return file;
    }
}
