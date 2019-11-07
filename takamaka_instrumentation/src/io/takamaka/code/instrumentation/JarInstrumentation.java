package io.takamaka.code.instrumentation;

import java.io.IOException;
import java.nio.file.Path;

import io.takamaka.code.instrumentation.internal.JarInstrumentationImpl;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedJar;

/**
 * An instrumenter of a jar file. It generates another jar file that
 * contains the same classes as the former, but instrumented. This means
 * for instance that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public interface JarInstrumentation {

	/**
	 * Instruments the given jar file into another jar file. This instrumentation
	 * might fail if at least a class did not verify.
	 * 
	 * @param verifiedJar the jar that contains the classes already verified
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @param destination the jar file to generate
	 * @throws IOException if there was a problem accessing the classes on disk
	 * @throws VerificationException if {@code verifiedJar} has some error
	 */
	static JarInstrumentation of(VerifiedJar verifiedJar, GasCostModel gasCostModel, Path destination) throws IOException {
		return new JarInstrumentationImpl(verifiedJar, gasCostModel, destination);
	}
}