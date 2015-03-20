package yajco.generator;

import java.util.Properties;
import javax.annotation.processing.Filer;
import yajco.model.Language;

public interface FilesGenerator {
    public void generateFiles(Language language,Filer filer, Properties properties);
}
