package yajco.xtext.commons.settings;

import org.eclipse.xtext.xtext.wizard.LanguageDescriptor;

import java.util.Properties;

public class XtextProjectSettings {

    private static final String DEFAULT_LANGUAGE_BASE_NAME = "org.example.mydsl";
    private static final String DEFAULT_FILE_EXTENSION = "mydsl";

    private static String LANGUAGE_BASE_NAME;
    private static LanguageDescriptor.FileExtensions FILE_EXTENSION;
    private static String MAIN_NODE;
    private static String CODE_RUNNER;

    private static final String XTEXT = "yajco.xtext";
    private static final String LANGUAGE_BASE_NAME_KEY = XTEXT + ".baseName";
    private static final String FILE_EXTENSION_KEY = XTEXT + ".fileExtension";
    private static final String CODE_RUNNER_KEY = XTEXT + ".codeRunner";
    private static final String YAJCO_MAINNODE_KEY = "yajco.mainNode";

    private static final String XTEXT_VERSION = "2.16.0";
    private static final String TARGET_PATH = "target/xtext";
    private static final String XTEXT_FILE_EXTENSION = ".xtext";

    private static final XtextProjectSettings INSTANCE = new XtextProjectSettings();

    public static XtextProjectSettings getInstance(){
        return INSTANCE;
    }

    public String getCodeRunner() {
        return CODE_RUNNER;
    }

    public void init(Properties properties){
        System.out.println("Initializing projects settings ...");
        LANGUAGE_BASE_NAME = properties.getProperty(LANGUAGE_BASE_NAME_KEY, DEFAULT_LANGUAGE_BASE_NAME);
        FILE_EXTENSION = LanguageDescriptor.FileExtensions.fromString(
                properties.getProperty(FILE_EXTENSION_KEY, DEFAULT_FILE_EXTENSION));
        String main = properties.getProperty(YAJCO_MAINNODE_KEY, null);
        MAIN_NODE = main != null && main.lastIndexOf(".") != -1 ? main.substring(main.lastIndexOf(".")+1) : main;
        CODE_RUNNER = properties.getProperty(CODE_RUNNER_KEY, null);
    }

    private XtextProjectSettings(){

    }

    public String getLanugageFullName(){
        return LANGUAGE_BASE_NAME + "." + MAIN_NODE;
    }

    public String getLanguageBaseName(){
        return LANGUAGE_BASE_NAME;
    }

    public String getMainNode(){
        return MAIN_NODE;
    }

    public LanguageDescriptor.FileExtensions getFileExtension(){
        return FILE_EXTENSION;
    }

    public String getXtextVersion(){
        return XTEXT_VERSION;
    }

    public String getTargetPath(){
        return TARGET_PATH;
    }

    public String getRuntimeProjectGrammarPath() {
        return getTargetPath() + "/" + getLanguageBaseName() + ".parent/" +
                getLanguageBaseName() + "/src/" + getLanguageBaseName().replaceAll("\\.", "/") +
                "/" + MAIN_NODE + XTEXT_FILE_EXTENSION;
    }

    public String getRuntimeProjectQNPPath() {
        return getTargetPath() + "/" + getLanguageBaseName() + ".parent/" +
                getLanguageBaseName() + "/src/" + getLanguageBaseName().replaceAll("\\.", "/") +
                "/" + MAIN_NODE + "QNP.java";
    }

    public String getRuntimeProjectRuntimeModulePath() {
        return getTargetPath() + "/" + getLanguageBaseName() + ".parent/" +
                getLanguageBaseName() + "/src/" + getLanguageBaseName().replaceAll("\\.", "/") +
                "/" + MAIN_NODE  + "RuntimeModule.xtend";
    }

    public String getGrammarName(){
        return Character.toLowerCase(MAIN_NODE.charAt(0)) + MAIN_NODE.substring(1);
    }

    public String getParentProjectPomPath(){
        return TARGET_PATH + "/" + getLanguageBaseName() + ".parent/pom.xml";
    }

    public String getRuntimeProjectPomPath(){
        return TARGET_PATH + "/" + getLanguageBaseName() + ".parent/" + getLanguageBaseName() + "/pom.xml";
    }

    public String getIdeProjectPomPath(){
        return TARGET_PATH + "/" + getLanguageBaseName() +  ".parent/" + getLanguageBaseName() + ".ide/pom.xml";
    }

    public String getUiProjectPomPath(){
        return TARGET_PATH + "/" + getLanguageBaseName() +  ".parent/" + getLanguageBaseName() + ".ui/pom.xml";
    }

}