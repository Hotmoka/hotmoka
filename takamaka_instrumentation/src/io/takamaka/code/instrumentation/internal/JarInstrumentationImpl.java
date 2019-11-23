package io.takamaka.code.instrumentation.internal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import io.takamaka.code.instrumentation.GasCostModel;
import io.takamaka.code.instrumentation.JarInstrumentation;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.VerifiedJar;

/**
 * An instrumenter of a jar file. It generates another jar file that
 * contains the same classes as the former, but instrumented. This means
 * for instance that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public class JarInstrumentationImpl implements JarInstrumentation {

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
	public JarInstrumentationImpl(VerifiedJar verifiedJar, GasCostModel gasCostModel, Path destination) throws IOException {
		if (verifiedJar.hasErrors())
			throw new VerificationException(verifiedJar.getFirstError().get());

		// instrumentation and dump of the class files
		try (JarOutputStream instrumentedJar = new JarOutputStream(new FileOutputStream(destination.toFile()))) {
			// we cannot proceed in parallel since the BCEL library is not thread-safe
			verifiedJar.classes().forEach(clazz -> dumpInstrumentedClass(clazz, gasCostModel, instrumentedJar));
		}
	}

	/**
	 * Instruments the given class from a jar file.
	 * 
	 * @param clazz the class
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @param instrumentedJar the jar where the instrumented class must be dumped
	 */
	private void dumpInstrumentedClass(VerifiedClass clazz, GasCostModel gasCostModel, JarOutputStream instrumentedJar) {
		try {
			// add the same entry to the resulting jar
			instrumentedJar.putNextEntry(new JarEntry(clazz.getClassName().replace('.', '/') + ".class"));
	
			// instrument the class and dumps it into the jar file
			new InstrumentedClass(clazz, gasCostModel).dump(instrumentedJar);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}