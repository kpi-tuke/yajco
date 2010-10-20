package yajco.generator;

import java.io.File;
import yajco.model.Language;

public interface FilesGenerator {
    public void generateFiles(Language language,File directory);
}
