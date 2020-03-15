package io.takamaka.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.hotmoka.nodes.GasCostModel;
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
	void translateTwice() throws IOException {
		Path origin = Paths.get("../io-takamaka-examples/target/io-takamaka-examples-1.0-lambdas.jar");
		URL classpath = new File("../io-takamaka-code/target/io-takamaka-code-1.0.jar").toURI().toURL();
    	TakamakaClassLoader classLoader = TakamakaClassLoader.of(new URL[] { classpath, origin.toUri().toURL() });
    	VerifiedJar verifiedJar = VerifiedJar.of(origin, classLoader, false);
    	GasCostModel costModel = GasCostModel.standard();
		InstrumentedJar.of(verifiedJar, costModel);
    	InstrumentedJar.of(verifiedJar, costModel);
	}
}