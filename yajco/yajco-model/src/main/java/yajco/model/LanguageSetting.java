package yajco.model;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import yajco.annotation.Before;
import yajco.annotation.Token;
import yajco.annotation.printer.NewLine;

/**
 * Mirror class for Java Properties
 *
 * @author DeeL
 */
public class LanguageSetting {

    private String name;
    private String value;

    @NewLine
    public LanguageSetting(String name, @Before("=") @Token("STRING_VALUE") String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public static Set<LanguageSetting> convertToLanguageSetting(Properties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Properties cannot be a NULL value!");
        }
        Set<LanguageSetting> settings = new HashSet<LanguageSetting>(properties.size());
        for (String name : properties.stringPropertyNames()) {
            LanguageSetting setting = new LanguageSetting(name, properties.getProperty(name));
            settings.add(setting);
        }
        return settings;
    }
    
    public static Properties convertToJavaProperties(Set<LanguageSetting> settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be a NULL value!");
        }
        Properties properties = new Properties();
        for (LanguageSetting languageSetting : settings) {
            properties.setProperty(languageSetting.getName(), languageSetting.getValue());
        }
        return properties;
    }
}
