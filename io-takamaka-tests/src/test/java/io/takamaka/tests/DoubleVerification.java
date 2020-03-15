package io.takamaka.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;

/**
 * This test tries to verify the same jar twice. This checks
 * that verification does not modify static information of the verified jar,
 * hence it can be applied twice without exceptions.
 */
class DoubleVerification {

	@Test
	void verifyTwice() throws IOException {
		Path origin = Paths.get("../io-takamaka-examples/target/io-takamaka-examples-1.0-lambdas.jar");
		URL classpath = new File("../io-takamaka-code/target/io-takamaka-code-1.0.jar").toURI().toURL();
    	TakamakaClassLoader classLoader = TakamakaClassLoader.of(new URL[] { classpath, origin.toUri().toURL() });
    	VerifiedJar.of(origin, classLoader, false);
    	VerifiedJar.of(origin, classLoader, false);
	}
}