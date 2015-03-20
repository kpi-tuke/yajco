package yajco.generator.xmlserializer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import yajco.generator.AbstractFileGenerator;
import yajco.generator.GeneratorException;
import yajco.model.Language;
import yajco.model.utilities.XMLLanguageFormatHelper;

/**
 *
 * @author DeeL
 */
public class XMLserializer extends AbstractFileGenerator{
    private static final String PROPERTY_ENABLER = "xml";

    @Override
    public void generate(Language language, Writer writer) {
        XMLLanguageFormatHelper.writeToXML(language, writer);
    }

    @Override
    protected boolean shouldGenerate() {
        // skopirovana implementacia od ineho... ten nech funguje stale
//        String option = properties.getProperty(GeneratorHelper.GENERATE_TOOLS_KEY, "").toLowerCase();
//        if (!option.contains("all") && !option.contains(PROPERTY_ENABLER) && !option.contains("printer")) {
//            System.out.println(getClass().getCanonicalName()+": Visitor not generated - property disabled (set "+GeneratorHelper.GENERATE_TOOLS_KEY+" to '"+PROPERTY_ENABLER+"' or 'all')");
//            return false;
//        }
        return true;
    }

    @Override
    public String getPackageName() {
        return "META-INF";
    }

    @Override
    public String getFileName() {
        return "yajco-lang.xml";
    }

    @Override
    public String getClassName() {
        return "";
    }
    
    @Override
    protected File getFileToWrite(Language language, Filer filer, String packageName, String fileName, boolean isSource) {
        FileObject fileObject;
        try {
            //boolean usePackageName = packageName != null && !packageName.isEmpty();
            //tento riadok je zmeneny
                
            fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT,"",packageName+"/"+fileName);
            fileObject.openWriter().close();
        } catch (IOException ex) {
            throw new GeneratorException("cannot create file " + packageName + "." + fileName, ex);
        }

        return new File(fileObject.toUri());
    }
    
    
}
