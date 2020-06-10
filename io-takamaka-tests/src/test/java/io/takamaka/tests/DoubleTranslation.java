package io.takamaka.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
		Path origin = Paths.get("../io-takamaka-examples/target/io-takamaka-examples-1.0.0-lambdas.jar");
		Path classpath = Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.0.jar");
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