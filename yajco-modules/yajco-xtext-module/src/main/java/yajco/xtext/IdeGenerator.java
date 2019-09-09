package yajco.xtext;

import com.google.common.base.Charsets;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.xtext.util.Files;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.util.XtextVersion;
import org.eclipse.xtext.xtext.wizard.*;
import org.eclipse.xtext.xtext.wizard.cli.CliProjectsCreator;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.model.*;
import yajco.model.pattern.impl.Identifier;
import yajco.xtext.commons.maven.MavenRunner;
import yajco.xtext.commons.model.XtextGrammarModel;
import yajco.xtext.commons.settings.XtextProjectSettings;
import yajco.xtext.grammar.XtextGrammarPrinter;
import yajco.xtext.semantics.SemanticsGenerator;

import javax.annotation.processing.Filer;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


public class IdeGenerator implements FilesGenerator {
    private static final String PROPERTY_ENABLER = "ide";
    private static final String GENERATE_TOOLS_KEY = "yajco.generateTools";

    private XtextProjectSettings settings;

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {

        String option = properties.getProperty(GENERATE_TOOLS_KEY, "").toLowerCase();
        if (!option.contains("all") && !option.contains(PROPERTY_ENABLER)) {
            System.out.println(getClass().getCanonicalName() +
                    ": Textual language representation not generated - property disabled (set " +
                    GENERATE_TOOLS_KEY + " to '" + PROPERTY_ENABLER + "' or 'all')");
            return;
        }

        System.out.println("Starting generation of the development environments ...");
        this.settings = XtextProjectSettings.getInstance();
        this.settings.init(properties);

        createXtextProjects();
        XtextGrammarModel grammarModel = createGrammarModel(language);
        printXtextGrammar(grammarModel);

        try {
            MavenRunner.executeMavenCompile();
        } catch (MavenInvocationException e) {
            throw new GeneratorException("Action `mvn compile` on parent Xtext project failed.", e);
        }
        printQNPFiles(language);
        updatePomFiles();

        if (settings.getCodeRunner() != null) {
            System.out.println("Generation of the code runner for Eclipse enabled ... ");
            new SemanticsGenerator().generateFiles();
        }else {
            System.out.println("Generation of the code runner for Eclipse disabled ... ");
        }
    }

    private void printQNPFiles(Language language) {
        System.out.println("Generating QualifiedNameProvider class ... ");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(settings.getRuntimeProjectRuntimeModulePath(), false));
            printQNPClass(new PrintWriter(writer));
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write to the " + settings.getRuntimeProjectRuntimeModulePath() + " file.", ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new GeneratorException("Cannot close the " + settings.getRuntimeProjectRuntimeModulePath() + " file.", e);
                }
            }
        }

        writer = null;
        try {
            File file = new File(settings.getRuntimeProjectQNPPath());
            file.createNewFile();
            writer = new BufferedWriter(new FileWriter(file, false));
            printCustomQNP(new PrintWriter(writer), language);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write to the " + settings.getRuntimeProjectQNPPath() + " file.", ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new GeneratorException("Cannot close the " + settings.getRuntimeProjectQNPPath() + " file.", e);
                }
            }
        }
    }

    private void printXtextGrammar(XtextGrammarModel grammarModel) {
        System.out.println("Printing Xtext grammar ...");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(settings.getRuntimeProjectGrammarPath(), false));
            XtextGrammarPrinter printer = new XtextGrammarPrinter(grammarModel);
            printer.print(new PrintWriter(writer));
        } catch (IOException ex) {
            throw new GeneratorException("Cannot print Xtext grammar.", ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new GeneratorException("Cannot close Xtext grammar file.", e);
                }
            }
        }
    }

    private XtextGrammarModel createGrammarModel(Language language) {
        System.out.println("Creating Xtext grammar model ...");
        XtextGrammarModel grammarModel = new XtextGrammarModel(language);
        grammarModel.createModel();
        return grammarModel;
    }

    private void createXtextProjects() {
        WizardConfiguration wizardConfiguration = new WizardConfiguration();
        wizardConfiguration.setBaseName(settings.getLanguageBaseName());
        wizardConfiguration.getLanguage().setName(settings.getLanugageFullName());
        wizardConfiguration.getLanguage().setFileExtensions(settings.getFileExtension());
        wizardConfiguration.getRuntimeProject().setEnabled(true);
        wizardConfiguration.getIdeProject().setEnabled(true);
        wizardConfiguration.getUiProject().setEnabled(true);

        wizardConfiguration.setRootLocation(settings.getTargetPath());
        wizardConfiguration.setXtextVersion(new XtextVersion(settings.getXtextVersion()));
        wizardConfiguration.setEncoding(Charsets.UTF_8);
        wizardConfiguration.setPreferredBuildSystem(BuildSystem.MAVEN);
        wizardConfiguration.setSourceLayout(SourceLayout.PLAIN);
        wizardConfiguration.setProjectLayout(ProjectLayout.HIERARCHICAL);
        wizardConfiguration.setJavaVersion(JavaVersion.JAVA8);
        wizardConfiguration.setLanguageServer(LanguageServer.FATJAR);

        CliProjectsCreator creator = new CliProjectsCreator();
        creator.setLineDelimiter("\n");

        try {
            File targetLocation = new File(settings.getTargetPath());
            targetLocation.mkdirs();
            Files.sweepFolder(targetLocation);
            wizardConfiguration.setRootLocation(targetLocation.getPath());
            creator.createProjects(wizardConfiguration);
            System.out.println("Creating Xtext projects ... ");
            System.out.println("Language name: " + wizardConfiguration.getLanguage().getName());
            System.out.println("Language file extension: " + wizardConfiguration.getLanguage().getFileExtensions().toString());

        } catch (FileNotFoundException e) {
            throw new GeneratorException("Cannot create Xtext projects.", e);
        }
    }

    private Model getMavenModel(String path) {
        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            File file = new File(path);
            reader = new FileReader(file);
            model = mavenreader.read(reader);
            model.setPomFile(file);
        } catch (Exception ex) {
            throw new GeneratorException("Cannot read " + path + " file.", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new GeneratorException("Cannot close " + path + " file reader.", e);
                }
            }
        }
        return model;
    }

    private void updatePomFiles() {
        System.out.println("Updating POM files ...");
        String path = settings.getParentProjectPomPath();

        Model model = getMavenModel(path);

        MavenProject project = new MavenProject(model);
        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-resources-plugin");
        plugin.setVersion("2.6");

        PluginExecution execution = new PluginExecution();
        execution.setId("copy");
        execution.setPhase("package");
        execution.setGoals(Arrays.asList("copy-resources"));

        StringBuilder configString = new StringBuilder()
                .append("<configuration><outputDirectory>").append("${basedir}/../target/eclipsePlugin").append("</outputDirectory>")
                .append("<resources>")
                .append("<resource><directory>" + "${basedir}/target</directory>")
                .append("<includes><include>*.jar</include></includes><excludes><exclude>*-ls.jar</exclude></excludes>")
                .append("</resource>")
                .append("</resources></configuration>");

        Xpp3Dom dom = null;
        try {
            dom = Xpp3DomBuilder.build(new StringReader(configString.toString()));
        } catch (XmlPullParserException | IOException ex) {
            throw new GeneratorException("Cannot create copy-resources configuration for the " + path + "file.", ex);
        }

        execution.setConfiguration(dom);

        PluginExecution execution1 = new PluginExecution();
        execution1.setId("copy1");
        execution1.setPhase("package");
        execution1.setGoals(Arrays.asList("copy-resources"));


        StringBuilder config = new StringBuilder()
                .append("<configuration><outputDirectory>").append("${basedir}/../target/languageServer").append("</outputDirectory>")
                .append("<resources><resource>")
                .append("<directory>")
                .append("${basedir}/target</directory>")
                .append("<includes><include>*-ls.jar</include></includes>")
                .append("</resource></resources></configuration>");


        dom = null;
        try {
            dom = Xpp3DomBuilder.build(new StringReader(config.toString()));
        } catch (XmlPullParserException | IOException e) {
            throw new GeneratorException("Cannot create copy-resources configuration for the " + path + "file.", e);
        }
        execution1.setConfiguration(dom);

        plugin.getExecutions().add(execution);
        plugin.getExecutions().add(execution1);
        project.getPluginManagement().getPlugins().add(plugin);

        writeMavenModelToPom(path, project.getModel());

        modifyChildPom(XtextProjectSettings.getInstance().getRuntimeProjectPomPath());
        modifyChildPom(XtextProjectSettings.getInstance().getIdeProjectPomPath());
        modifyChildPom(XtextProjectSettings.getInstance().getUiProjectPomPath());

    }

    private void writeMavenModelToPom(String path, Model model) {
        MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
        FileWriter writer = null;
        try {
            File file = new File(path);
            writer = new FileWriter(file);
            mavenXpp3Writer.write(writer, model);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write to " + path + "file.", ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new GeneratorException("Cannot close " + path + "file.", e);
                }
            }
        }
    }


    private void modifyChildPom(String pomPath) {
        Model model = getMavenModel(pomPath);

        MavenProject project = new MavenProject(model);
        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-resources-plugin");
        project.getBuildPlugins().add(plugin);

        writeMavenModelToPom(pomPath, project.getModel());
    }

    private void printCustomQNP(PrintWriter printWriter, Language language) {
        printWriter.print("package " + XtextProjectSettings.getInstance().getLanguageBaseName() + ";\n\n");
        printWriter.print("import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;\n" +
                "import org.eclipse.xtext.naming.QualifiedName;\n\n");
        printWriter.print("public class " + XtextProjectSettings.getInstance().getMainNode() +
                "QNP extends DefaultDeclarativeQualifiedNameProvider{\n");
        printQNPBody(printWriter, language.getConcepts());
        printWriter.print("}");
    }

    private void printQNPBody(PrintWriter printWriter, List<Concept> concepts) {
        for (Concept concept : concepts) {
            for (Property property : concept.getAbstractSyntax()) {
                PropertyReferencePart part = getNotationPartByName(property, concept.getConcreteSyntax().get(0));
                if (part != null) {
                    printWriter.print("\tQualifiedName qualifiedName(" + settings.getLanguageBaseName() + "." +
                            getGrammarPackage(settings.getMainNode()) + "." + makeCamelCaseName(concept.getName()) + " " +
                            Character.toLowerCase(concept.getName().charAt(0)) + ") {\n");
                    printWriter.print("\t\treturn QualifiedName.create(" + Character.toLowerCase(concept.getName().charAt(0)) +
                            ".get" + Character.toUpperCase(part.getProperty().getName().charAt(0)) + part.getProperty().getName().substring(1) +
                            "());\n");
                    printWriter.print("\t}\n\n");
                    break;
                }
            }
        }
    }

    private String makeCamelCaseName(String name) {
        if (name != null) {
            List<String> strings = Arrays.asList(name.split("\\."));
            return String.join("", strings.stream().map(this::toCapital).collect(Collectors.toList()));
        }

        return null;
    }

    private String toCapital(String cls) {
        if (cls != null && !cls.isEmpty()) {
            return Character.toUpperCase(cls.charAt(0)) + cls.substring(1);
        } else
            return "";
    }

    private PropertyReferencePart getNotationPartByName(Property property, Notation notation) {
        return (PropertyReferencePart) notation.getParts().stream().filter(notationPart -> notationPart instanceof PropertyReferencePart &&
                (property.getPattern(Identifier.class) != null)).findFirst().orElse(null);
    }

    private String getGrammarPackage(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Grammar";
    }

    private void printQNPClass(PrintWriter printWriter) {
        String mainNode = settings.getMainNode();
        printWriter.print("package " + settings.getLanguageBaseName() + "\n" +
                "\n" +
                "import org.eclipse.xtext.naming.IQualifiedNameProvider\n" +
                "\n" +
                "/**\n" +
                " * Use this class to register components to be used at runtime / without the Equinox extension registry.\n" +
                " */\n" +
                "class " + mainNode + "RuntimeModule extends Abstract"
                + mainNode + "RuntimeModule {\n" +
                "\t\n" +
                "\t\n" +
                "    override Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {\n" +
                "        return " + mainNode + "QNP;\n" +
                "    }\n" +
                "}\n");
    }

}
