package yajco.generator.parsergen;

import yajco.generator.FilesGenerator;
import yajco.model.Language;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface CompilerGenerator extends FilesGenerator {

    /**
     * Collection to store parser service providers during processing.
     * This is used to batch changes to the service file and write them all at once
     * at the end of annotation processing.
     */
    Set<String> PARSER_SERVICE_PROVIDERS = new HashSet<>();

    /**
     * Registers the given class as the service provider of the {@link Parser} service as per the SPI contract.
     * This method collects parser names during processing and they will be written at the end of annotation processing.
     *
     * @param parserFQN fully qualified name of the class implementing {@link Parser}
     * @param filer     filer used to create or access resources (not used in this method, but kept for compatibility)
     * @see java.util.ServiceLoader
     */
    static void registerParserServiceProvider(String parserFQN, Filer filer) {
        // Add the parser to the collection for later processing
        PARSER_SERVICE_PROVIDERS.add(parserFQN);
    }

    /**
     * Writes all collected parser service providers to the service file.
     * This method should be called at the end of annotation processing.
     *
     * @param filer filer used to create resources
     * @throws IOException if the file cannot be created or an I/O error occurs
     */
    static void writeParserServiceProviders(Filer filer) throws IOException {
        if (PARSER_SERVICE_PROVIDERS.isEmpty()) {
            return;
        }

        // Use the standard SPI filename for proper ServiceLoader integration
        String serviceFile = "META-INF/services/" + Parser.class.getName();

        // Create the service file
        FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, "", serviceFile);
        try (OutputStreamWriter out = new OutputStreamWriter(fo.openOutputStream(), UTF_8)) {
            for (String parserFQN : PARSER_SERVICE_PROVIDERS) {
                out.write(parserFQN);
                out.write(System.lineSeparator());
            }
        }
    }

    void generateFiles(Language language, Filer filer, Properties properties, String parserClassName);

}
