package yajco.generator.parsergen;

import yajco.generator.FilesGenerator;
import yajco.model.Language;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface CompilerGenerator extends FilesGenerator {

	/**
	 * Registers the given class as the service provider of the {@link Parser} service as per the SPI contract.
	 *
	 * @param parserFQN fully qualified name of the class implementing {@link Parser}
	 * @param filer     filer used to
	 * @throws IOException if the file cannot be created or an I/O error occurs
	 * @see java.util.ServiceLoader
	 */
	static void registerParserServiceProvider(String parserFQN, Filer filer) throws IOException {
		FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, "" ,
				"META-INF/services/" + Parser.class.getName());
		try (OutputStreamWriter out = new OutputStreamWriter(fo.openOutputStream(), UTF_8)) {
			out.write(parserFQN);
		}
	}

	void generateFiles(Language language, Filer filer, Properties properties, String parserClassName);

}
