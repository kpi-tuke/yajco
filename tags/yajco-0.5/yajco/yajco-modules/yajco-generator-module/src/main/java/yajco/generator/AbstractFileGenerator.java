package yajco.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import yajco.generator.util.Utilities;
import yajco.model.Language;

public abstract class AbstractFileGenerator implements FilesGenerator {

    protected Properties properties;

    public abstract void generate(Language language, Writer writer);

    protected abstract boolean shouldGenerate();

    public abstract String getPackageName();

    public abstract String getFileName();

    public abstract String getClassName();

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
        this.properties = properties;

        if (shouldGenerate()) {
            File file = getFileToWrite(language, filer, getPackageName(), getClassName());
            generate(language, file);
        }
    }

    public void generate(Language language, File file) {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
            generate(language, writer);
            Utilities.formatCode(file);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write to file " + file.getAbsolutePath() + " (" + ex.getMessage() + ")", ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                throw new GeneratorException("Cannot close writer for file " + file.getAbsolutePath(), ex);
            }
        }
    }

    protected File getFileToWrite(Language language, Filer filer, String packageName, String fileName) {
//        if (!directory.isDirectory()) {
//            throw new GeneratorException("Supplied file argument is not directory: " + directory.getAbsolutePath());
//        }
        //String filePath = yajco.model.utilities.Utilities.getLanguagePackageName(language).replace('.', File.separatorChar) + File.separator + packageName + File.separator + fileName;
        //File file = new File(directory, filePath);
        //Utilities.createDirectories(file, false);
        FileObject fileObject;
        try {
            fileObject = filer.createSourceFile(yajco.model.utilities.Utilities.getLanguagePackageName(language) + "." + packageName + "." + fileName);
            fileObject.openWriter().close();
        } catch (IOException ex) {
            throw new GeneratorException("cannot create file " + packageName + "." + fileName, ex);
        }

        return new File(fileObject.toUri());
    }
}
