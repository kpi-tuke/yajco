package yajco.generator.parsergen;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import yajco.generator.GeneratorException;

public class Conversions {

	public static final String PROPERTIES_FILE = "conversions.properties";
	public static final String PROPERTIES_FILE_PROPERTY = "yajco.parsergen.conversions";
	private final Map<String, Conversion> conversions = new HashMap<String, Conversion>();

	public Conversions() {
		//Register primitive types conversions
		putConversion(new Conversion("boolean", "Boolean.parseBoolean(%s)", "false"));
		putConversion(new Conversion("byte", "Byte.parseByte(%s)", "(byte)0"));
		putConversion(new Conversion("short", "Short.parseShort(%s)", "(short)0"));
		putConversion(new Conversion("int", "Integer.parseInt(%s)", "0"));
		putConversion(new Conversion("long", "Long.parseLong(%s)", "0L"));
		putConversion(new Conversion("float", "Float.parseFloat(%s)", "0.0F"));
		putConversion(new Conversion("double", "Double.parseDouble(%s)", "0.0"));
		putConversion(new Conversion("char", "%s.charAt(0)", "'\\0'"));

		//Add custom conversions from configuration file
		Properties properties = new Properties();
		try {
			String propertiesFile = System.getProperty(PROPERTIES_FILE_PROPERTY, PROPERTIES_FILE);
			properties.load(getClass().getResourceAsStream(propertiesFile));
			for (String type : properties.stringPropertyNames()) {
				putConversion(new Conversion(type, properties.getProperty(type)));
			}
		} catch (IOException e) {
			e.printStackTrace();
			new GeneratorException("Cannot load " + PROPERTIES_FILE, e);
		}
	}

	private void putConversion(Conversion conversion) {
		conversions.put(conversion.getType(), conversion);
	}

	public boolean containsConversion(String type) {
		return conversions.containsKey(type);
	}

	public String getConversion(String type) {
		return conversions.get(type).getConversion();
	}

	public String getDefaultValue(String type) {
		return conversions.get(type).getDefaultValue();
	}

	private static class Conversion {

		private final String type;
		private final String conversion;
		private final String defaultValue;

		private Conversion(String type, String conversion) {
			this(type, conversion, "null");
		}

		private Conversion(String type, String conversion, String defaultValue) {
			this.type = type;
			this.conversion = conversion;
			this.defaultValue = defaultValue;
		}

		private String getType() {
			return type;
		}

		private String getConversion() {
			return conversion;
		}

		private String getDefaultValue() {
			return defaultValue;
		}
	}
}
