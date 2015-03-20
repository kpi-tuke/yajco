package yajco.model.utilities;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import yajco.model.Language;
import yajco.model.YajcoModelElement;

/**
 *
 * @author DeeL
 */
public class XMLLanguageFormatHelper {
    
    public final static String YAJCO_XML_FILE_NAME = "yajco-lang.xml";
    public final static String YAJCO_XML_FILE_DIR = "META-INF";

    private final static XStream xstream = new XStream();

    static {
        xstream.omitField(YajcoModelElement.class, "sourceElement");
    }

    public static void writeToXML(Language language, Writer writer) {
        xstream.toXML(language, writer);
    }

    public static void writeToXML(Language language, OutputStream stream) {
        xstream.toXML(language, stream);
    }

    public static String writeToXML(Language language) {
        return xstream.toXML(language);
    }

    public static Language readFromXML(Reader reader) {
        Object lang = xstream.fromXML(reader);
        return testLanguage(lang);
    }

    public static Language readFromXML(InputStream stream) {
        Object lang = xstream.fromXML(stream);
        return testLanguage(lang);
    }

    public static Language readFromXML(String input) {
        Object lang = xstream.fromXML(input);
        return testLanguage(lang);
    }
    
    public static List<Language> getAllLanguagesFromXML() {
        try {
            List<InputStream> inputStreams = loadResources(YAJCO_XML_FILE_DIR+"/"+YAJCO_XML_FILE_NAME, XMLLanguageFormatHelper.class.getClassLoader());
            List<Language> languages = new ArrayList<Language>();
            for (InputStream inputStream : inputStreams) {
                languages.add(readFromXML(inputStream));
            }
            return languages;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Language testLanguage(Object lang) throws IllegalArgumentException {
        if (lang instanceof Language) {
            return (Language) lang;
        } else {
            throw new IllegalArgumentException("Provided input does not contain Language in XML format");
        }
    }
    
    private static List<InputStream> loadResources(String name, ClassLoader classLoader) throws IOException {
        final List<InputStream> list = new ArrayList<InputStream>();
        final Enumeration<URL> systemResources =
                (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
                .getResources(name);
        while (systemResources.hasMoreElements()) {
            list.add(systemResources.nextElement().openStream());
        }
        return list;
    }
}
