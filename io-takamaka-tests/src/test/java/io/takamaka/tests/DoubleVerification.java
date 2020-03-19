package io.takamaka.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
		Path classpath = Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar");
		byte[] bytesOfOrigin = Files.readAllBytes(origin);
		byte[] bytesOfClasspath = Files.readAllBytes(classpath);
    	TakamakaClassLoader classLoader = TakamakaClassLoader.of
    		(Stream.of(bytesOfClasspath, bytesOfOrigin),
			Stream.of("", "")); // names are irrelevant if we do not execute the code);
    	VerifiedJar.of(bytesOfOrigin, classLoader, false);
    	VerifiedJar.of(bytesOfOrigin, classLoader, false);
	}
}