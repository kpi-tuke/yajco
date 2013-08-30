package yajco.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import yajco.generator.FilesGenerator;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.generator.util.ServiceFinder;
import yajco.model.Language;

public class ParserHelper {

    private static final String PROPERTY_SETTINGS_FILE = "/yajco.properties";
    private Reader reader;
    private Properties properties;

    public ParserHelper(Reader reader, Properties properties) {
        this.reader = reader;
        if (properties == null) {
            this.properties = new Properties();
        } else {
            this.properties = properties;
        }
        loadProperties();
    }

    public ParserHelper(File file, Properties properties) throws IOException {
        this(new FileReader(file), properties);
    }

    private void loadProperties() {
        Properties loadedProperties = new Properties();
        //loadedProperties.setProperty("yajco.generator.classgen.ClassGenerator", "false");
        loadedProperties.setProperty("yajco.generateTools", "all");

        try {
            InputStream inputStream = this.getClass().getResourceAsStream(PROPERTY_SETTINGS_FILE);
            loadedProperties.load(inputStream);
            //logger.debug("Loaded config from file: {}",properties);
        } catch (Exception e) {
            // LOG but don't do anything, it is not error
            //logger.info("Cannot find or load {} file in classpath. Will use only @Parser options.", PROPERTY_SETTINGS_FILE);
            //logger.debug("Loading config file: {}",e.getLocalizedMessage());
            //throw new GeneratorException("Cannot load " + PROPERTY_SETTINGS_FILE, e);
        }
        loadedProperties.putAll(properties);
        properties = loadedProperties;
    }

    public Language parse() {
        try {
            YajcoParser parser = new YajcoParser();
            Language language = parser.parse(reader);
            return language;
        } catch (LALRParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Language parseAndGenerate() {
        return parseAndGenerate(null, true);
    }
    
    public Language parseAndGenerate(boolean generateCompiler) {
        return parseAndGenerate(null, generateCompiler);
    }
    
    public Language parseAndGenerate(String destinationDir, boolean generateCompiler) {
        Language language = parse();
        generate(destinationDir, language, generateCompiler);
        return language;
    }
    
    public void generate(Language language) {
        generate(null, language, true);
    }
    
    public void generate(String destinationDir, Language language) {
        generate(destinationDir, language, true);
    }
    
    public void generate(String destinationDir, Language language, boolean generateCompiler) {
        //System.out.println("Properties: "+properties);
        Filer filer = new MySimpleFiler(destinationDir);

        if (generateCompiler) {
            CompilerGenerator compilerGenerator = ServiceFinder.findCompilerGenerator();
            if (compilerGenerator != null) {
                compilerGenerator.generateFiles(language, filer, properties);
            }
        }
        
        Set<FilesGenerator> filesGenerators = ServiceFinder.findFilesGenerators(properties);
        for (FilesGenerator filesGenerator : filesGenerators) {
            filesGenerator.generateFiles(language, filer, properties);
        }
    }

    public class MySimpleFiler implements Filer {
        
        String parentDir = null;

        public MySimpleFiler() {
        }

        public MySimpleFiler(String parentDir) {
            this.parentDir = parentDir;
        }

        @Override
        public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements) throws IOException {
            return createFile("", name, JavaFileObject.Kind.SOURCE);
        }

        @Override
        public JavaFileObject createClassFile(CharSequence name, Element... originatingElements) throws IOException {
            return createFile("", name, JavaFileObject.Kind.CLASS);
        }

        @Override
        public FileObject createResource(Location location, CharSequence pkg, CharSequence relativeName, Element... originatingElements) throws IOException {
            return createFile(pkg, relativeName, JavaFileObject.Kind.OTHER);
        }

        @Override
        public FileObject getResource(Location location, CharSequence pkg, CharSequence relativeName) throws IOException {
            return createFile(pkg, relativeName, JavaFileObject.Kind.OTHER);
        }

        private JavaFileObject createFile(CharSequence pkg, CharSequence name, JavaFileObject.Kind kind) throws IOException {
            if (name == null || name.length() == 0) {
                throw new FilerException("Name cannot be null or empty.");
            }
            String path;
            if (kind == JavaFileObject.Kind.CLASS || kind == JavaFileObject.Kind.SOURCE) {
                path = name.toString().replace('.', File.separatorChar) + kind.extension;
            } else {
                if (pkg == null) {
                    pkg = "";
                }
                path = pkg.toString().replace('.', File.separatorChar) + File.separator + name;
            }
            if (parentDir == null) {
                parentDir = System.getProperty("user.dir");
            }
            URI uri = new File(parentDir, path).toURI();
            System.out.println("MyFiler opening file: " + uri);
            return new MyFileObject(uri, kind);
        }
    }

    class MyFileObject extends SimpleJavaFileObject {

        private File file;

        MyFileObject(URI uri, JavaFileObject.Kind kind) {
            super(uri, kind);
            file = new File(uri);
        }

        @Override
        public boolean delete() {
            return file.delete();
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            int character;
            StringBuilder stringBuilder = new StringBuilder();
            while ((character = reader.read()) >= 0) {
                stringBuilder.append((char) character);
            }
            reader.close();
            return stringBuilder;
        }

        @Override
        public long getLastModified() {
            return file.lastModified();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            file.getParentFile().mkdirs();
            return new FileOutputStream(file);
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            return new FileReader(file);
        }
    }
}
