package io.takamaka.code.instrumentation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.takamaka.code.instrumentation.internal.InstrumentedJarImpl;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedJar;

/**
 * An instrumented jar file, built from another, verified jar file. This means
 * for instance that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public interface InstrumentedJar {

	/**
	 * Instruments the given jar file into another jar file. This instrumentation
	 * might fail if at least a class did not verify.
	 * 
	 * @param verifiedJar the jar that contains the classes already verified
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @throws VerificationException if {@code verifiedJar} has some error
	 */
	static InstrumentedJar of(VerifiedJar verifiedJar, GasCostModel gasCostModel) {
		return new InstrumentedJarImpl(verifiedJar, gasCostModel);
	}

	/**
	 * Dumps this instrumented jar into a file.
	 * 
	 * @param destination the destination file where the jar must be dumped
	 * @throws IOException if an I/O error occurred
	 */
	void dump(Path destination) throws IOException;

	/**
	 * Yields the bytes of this jar.
	 * 
	 * @return the bytes
	 */
	byte[] toBytes();

	/**
	 * Yields the classes in this jar file.
	 * 
	 * @return the classes
	 */
	Stream<InstrumentedClass> classes();
}