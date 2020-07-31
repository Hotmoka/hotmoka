package io.takamaka.code.instrumentation.internal;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.takamaka.code.instrumentation.InstrumentedClass;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedJar;

/**
 * An instrumented jar file, built from another, verified jar file. This means
 * for instance that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public class InstrumentedJarImpl implements InstrumentedJar {

	/**
	 * The instrumented classes of the jar.
	 */
	private final SortedSet<InstrumentedClass> classes;

	/**
	 * Instruments the given jar file into another jar file. This instrumentation
	 * might fail if at least a class did not verify.
	 * 
	 * @param verifiedJar the jar that contains the classes already verified
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @throws VerificationException if {@code verifiedJar} has some error
	 */
	public InstrumentedJarImpl(VerifiedJar verifiedJar, GasCostModel gasCostModel) {
		if (verifiedJar.hasErrors())
			throw new VerificationException(verifiedJar.getFirstError().get());

		// we cannot proceed in parallel since the BCEL library is not thread-safe
		this.classes = verifiedJar.classes()
			.map(clazz -> InstrumentedClass.of(clazz, gasCostModel))
			.collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public void dump(Path destination) throws IOException {
		try (JarOutputStream instrumentedJar = new JarOutputStream(new FileOutputStream(destination.toFile()))) {
			classes.forEach(clazz -> dumpInstrumentedClass(clazz, instrumentedJar));
		}
		catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	@Override
	public byte[] toBytes() {
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		try (JarOutputStream instrumentedJar = new JarOutputStream(byteArray)) {
			classes.forEach(clazz -> dumpInstrumentedClass(clazz, instrumentedJar));
		}
		catch (UncheckedIOException e) {
			throw new IllegalStateException(e.getCause());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}

		return byteArray.toByteArray();
	}

	@Override
	public Stream<InstrumentedClass> classes() {
		return classes.stream();
	}

	/**
	 * Dumps the given class into a jar file.
	 * 
	 * @param instrumentedClass the class
	 * @param instrumentedJar the jar where the instrumented class must be dumped
	 */
	private static void dumpInstrumentedClass(InstrumentedClass instrumentedClass, JarOutputStream instrumentedJar) {
		try {
			// add the same entry to the resulting jar
			JarEntry entry = new JarEntry(instrumentedClass.getClassName().replace('.', '/') + ".class");
			entry.setTime(0L); // we set the timestamp to 0, so that the result is deterministic
			instrumentedJar.putNextEntry(entry);
	
			// dumps the class into the jar file
			instrumentedClass.toJavaClass().dump(instrumentedJar);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}