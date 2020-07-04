package io.takamaka.tests;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import io.takamaka.code.instrumentation.GasCostModel;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;

/**
 * This test tries to translate the same jar twice. This checks
 * that translation does not modify static information of the verified jar,
 * hence it can be applied twice without exceptions.
 */
class DoubleTranslation {

	@Test
	void translateTwice() throws IOException, XmlPullParserException {
		// we access the project.version property from the pom.xml file of the parent project
		MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("../pom.xml"));
        String version = (String) model.getProperties().get("project.version");
        Path origin = Paths.get("../io-takamaka-examples/target/io-takamaka-examples-" + version + "-lambdas.jar");
		Path classpath = Paths.get("../modules/explicit/io-takamaka-code-" + version + ".jar");
		byte[] bytesOfClasspath = Files.readAllBytes(classpath);
		byte[] bytesOfOrigin = Files.readAllBytes(origin);
		TakamakaClassLoader classLoader = TakamakaClassLoader.of(Stream.of(bytesOfClasspath, bytesOfOrigin),
			(name, pos) -> {}); // irrelevant if we do not execute the code
    	VerifiedJar verifiedJar = VerifiedJar.of(bytesOfOrigin, classLoader, false);
    	GasCostModel costModel = GasCostModel.standard();
		InstrumentedJar.of(verifiedJar, costModel);
    	InstrumentedJar.of(verifiedJar, costModel);
	}
}