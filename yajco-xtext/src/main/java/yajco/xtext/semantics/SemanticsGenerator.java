package yajco.xtext.semantics;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.texen.Generator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import yajco.generator.GeneratorException;
import yajco.model.Language;
import yajco.xtext.commons.settings.XtextProjectSettings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


public class SemanticsGenerator {
    private static final String TARGET_PATH = "target/xtext";
    private Language language;
    private MavenProject mavenProject = getMavenProject();

    public void generateFiles(Language language) {
        this.language = language;
        XtextProjectSettings settings = XtextProjectSettings.getInstance();

        changeUiManifest(settings);
        changeUiProject(settings);
        modifyPluginXml(settings);
        changeUiBuildProperties(settings);
    }

    private void changeUiBuildProperties(XtextProjectSettings settings) {
        FileOutputStream fileOutputStream = null;
        FileInputStream in = null;
        Properties properties = new Properties();
        try {
            in = new FileInputStream(buildUiPropsPath(settings));
            properties.load(in);
            in.close();
            String bin = properties.getProperty("bin.includes");
            String jarName = mavenProject.getArtifactId() + "-" + mavenProject.getVersion() + ".jar";
            properties.setProperty("bin.includes", bin + ",lib/" + jarName);
            properties.setProperty("src.includes", "lib/" + jarName);
            properties.setProperty("jars.extra.classpath", "lib/" + jarName);
            fileOutputStream = new FileOutputStream(buildUiPropsPath(settings));
            properties.store(fileOutputStream, null);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new GeneratorException("Cannot change build.properties file of ui Xtext project", e);
        }
    }

    private void changeUiManifest(XtextProjectSettings settings) {
        InputStream stream = null;
        FileOutputStream stream1 = null;
        try {
            stream = new FileInputStream(buildManifestUiPath(settings));

            Manifest manifest = new Manifest(stream);
            String current = manifest.getMainAttributes().getValue("Require-Bundle");
            manifest.getMainAttributes().putValue("Require-Bundle", current +
                    ",org.eclipse.ui.console;bundle-version=\"3.8.100\"");
            manifest.getMainAttributes().put(new Attributes.Name("Bundle-ClassPath"), "lib/"
                    + mavenProject.getArtifactId() + "-" + mavenProject.getVersion() + ".jar,\n .");
            stream1 = new FileOutputStream(buildManifestUiPath(settings));
            manifest.write(stream1);
        } catch (IOException e) {
            throw new GeneratorException("Cannot change MANIFEST.MF file of ui Xtext project", e);
        } finally {
            try {
                if (stream != null)
                    stream.close();
                if (stream1 != null) {
                    stream1.close();
                }
            } catch (IOException e) {
                throw new GeneratorException("Cannot change MANIFEST.MF file of ui Xtext project", e);
            }
        }
    }

    private void changeUiProject(XtextProjectSettings settings) {
        String srcPath = getUiSrcPath(settings);
        String path = (settings.getLanguageBaseName() + ".ui").replaceAll("\\.", "/");
        path += "/handler/" + "InterpretCodeHandler.java";
        File file = new File(srcPath + "/" + path);
        file.getParentFile().mkdirs();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(getHandlerCode(settings));
        } catch (IOException e) {
            throw new GeneratorException("Cannot generate InterpretCodeHandler.java class.", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new GeneratorException("Cannot generate InterpretCodeHandler.java class.", e);
                }
            }
        }

    }

    private MavenProject getMavenProject() {
        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();

        String path = System.getProperty("user.dir") + "/pom.xml";
        try {
            File file = new File(path);
            reader = new FileReader(file);
            model = mavenreader.read(reader);
            model.setPomFile(file);
        } catch (Exception ex) {
            throw new GeneratorException("Cannot read" + path + " file.", ex);
        }

        return new MavenProject(model);
    }

    private String buildManifestUiPath(XtextProjectSettings settings) {
        return TARGET_PATH + "/" + settings.getLanguageBaseName() + ".parent/" +
                settings.getLanguageBaseName() + ".ui/META-INF/MANIFEST.MF";
    }

    private String buildUiPropsPath(XtextProjectSettings settings) {
        return TARGET_PATH + "/" + settings.getLanguageBaseName() + ".parent/" +
                settings.getLanguageBaseName() + ".ui/build.properties";
    }

    private String getUiSrcPath(XtextProjectSettings settings) {
        return TARGET_PATH + "/" + settings.getLanguageBaseName() + ".parent/" +
                settings.getLanguageBaseName() + ".ui/src";
    }

    private String getUiPath(XtextProjectSettings settings) {
        return TARGET_PATH + "/" + settings.getLanguageBaseName() + ".parent/" +
                settings.getLanguageBaseName() + ".ui";
    }

    private void modifyPluginXml(XtextProjectSettings settings) {
        String path = getUiPath(settings) + "/plugin.xml";
        DocumentBuilder docBuilder = null;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(path);

            Node plugin = doc.getElementsByTagName("plugin").item(0);
            Element firstExtension = doc.createElement("extension");
            Element secondExtension = doc.createElement("extension");
            Element thirdExtension = doc.createElement("extension");

            firstExtension.setAttribute("point", "org.eclipse.ui.menus");
            secondExtension.setAttribute("point", "org.eclipse.ui.commands");
            thirdExtension.setAttribute("point", "org.eclipse.ui.handlers");

            Element menuContribution = doc.createElement("menuContribution");
            menuContribution.setAttribute("locationURI", "popup:#TextEditorContext?after=additions");
            Element command = doc.createElement("command");
            command.setAttribute("commandId", settings.getLanguageBaseName() + ".ui.handler.InterpreterCommand");
            command.setAttribute("style", "push");
            Element visibleWhen = doc.createElement("visibleWhen");
            visibleWhen.setAttribute("checkEnabled", "false");
            Element reference = doc.createElement("reference");
            reference.setAttribute("definitionId", settings.getLanugageFullName() + ".Editor.opened");

            visibleWhen.appendChild(reference);
            command.appendChild(visibleWhen);
            menuContribution.appendChild(command);
            firstExtension.appendChild(menuContribution);

            Element secondCommand = doc.createElement("command");
            secondCommand.setAttribute("name", "Interpret Code");
            secondCommand.setAttribute("id", settings.getLanguageBaseName() + ".ui.handler.InterpreterCommand");
            secondExtension.appendChild(secondCommand);

            Element thirdHandler = doc.createElement("handler");
            thirdHandler.setAttribute("class",
                    settings.getLanguageBaseName() + ".ui." + settings.getMainNode() + "ExecutableExtensionFactory:" +
                            settings.getLanguageBaseName() + ".ui.handler.InterpretCodeHandler");
            thirdHandler.setAttribute("commandId",
                    settings.getLanguageBaseName() + ".ui.handler.InterpreterCommand");
            thirdExtension.appendChild(thirdHandler);

            plugin.appendChild(firstExtension);
            plugin.appendChild(secondExtension);
            plugin.appendChild(thirdExtension);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(path));
            transformer.transform(source, result);

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            throw new GeneratorException("Cannot change plugin.xml file of the ui Xtext project.", e);
        }
    }

    private String getHandlerCode(XtextProjectSettings settings) {
        VelocityContext context = new VelocityContext();
        context.put("package", settings.getLanguageBaseName());
        context.put("codeRunner", language.getSetting("yajco.xtext.runCode"));
        VelocityEngine engine = new VelocityEngine();
        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "",
                new InputStreamReader(getClass().getResourceAsStream("/templates/InterpretCodeHandler.vm")));
        return writer.toString();
    }

}
