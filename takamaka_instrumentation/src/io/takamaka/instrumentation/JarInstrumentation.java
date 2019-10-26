package io.takamaka.instrumentation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.classfile.ClassParser;

import io.takamaka.instrumentation.internal.ClassInstrumentation;
import io.takamaka.instrumentation.internal.VerifiedClass;
import io.takamaka.instrumentation.issues.Issue;

/**
 * An instrumenter of a jar file. It generates another jar file that
 * contains the same classes as the former, but instrumented. This means
 * for instance that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public class JarInstrumentation {

	/**
	 * The ordered set of errors and warnings generated while verifying the classes of the jar.
	 */
	private final SortedSet<Issue> issues = new TreeSet<>();

	/**
	 * Verify and then instrument the given jar file into another jar file. This instrumentation
	 * might fail if at least a class did not verify. In that case, the issues generated
	 * during verification will contain at least an error.
	 * 
	 * @param origin the jar file to verify and instrument
	 * @param destination the jar file to generate
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 * @param duringInitialization true if and only if the code instrumentation occurs during
	 *                             blockchain initialization
	 * @throws IOException if there was a problem accessing the classes on disk
	 */
	public JarInstrumentation(Path origin, Path destination, TakamakaClassLoader classLoader, boolean duringInitialization) throws IOException {
		new Initializer(origin, destination, classLoader, duringInitialization);
	}

	/**
	 * Determines if the verification of at least one class of the jar failed with an error.
	 * 
	 * @return true if and only if that condition holds
	 */
	public final boolean hasErrors() {
		return getFirstError().isPresent();
	}

	/**
	 * Yields the first error (hence not a warning) that occurred during the verification of the origin jar.
	 */
	public Optional<io.takamaka.instrumentation.issues.Error> getFirstError() {
		return issues()
			.filter(issue -> issue instanceof io.takamaka.instrumentation.issues.Error)
			.map(issue -> (io.takamaka.instrumentation.issues.Error) issue)
			.findFirst();
	}

	/**
	 * Yields the issues generated during the verification of the classes of the origin jar.
	 * 
	 * @return the issues, in increasing order
	 */
	public Stream<Issue> issues() {
		return issues.stream();
	}

	/**
	 * Local scope to perform the instrumentation.
	 */
	private class Initializer {

		/**
		 * The jar file to instrument.
		 */
		private final JarFile originalJar;

		/**
		 * The resulting, instrumented jar file.
		 */
		private JarOutputStream instrumentedJar;

		/**
		 * The class loader that can be used to resolve the classes of the program.
		 */
		private final TakamakaClassLoader classLoader;

		/**
		 * True if and only if the code instrumentation occurs during.
		 * blockchain initialization.
		 */

		private final boolean duringInitialization;

		/**
		 * Performs the instrumentation of the given jar file into another jar file.
		 * 
		 * @param origin the jar file to instrument
		 * @param destination the jar file to generate
		 * @param classLoader the class loader that can be used to resolve the classes of the program,
		 *                    including those of {@code origin}
		 * @param duringInitialization true if and only if the instrumentation is performed during blockchain initialization
		 */
		private Initializer(Path origin, Path destination, TakamakaClassLoader classLoader, boolean duringInitialization) throws IOException {
			this.classLoader = classLoader;
			this.duringInitialization = duringInitialization;

			try {
				SortedSet<VerifiedClass> classes;

				// parsing and verification of the class files
				try (JarFile originalJar = this.originalJar = new JarFile(origin.toFile())) {
					// we cannot proceed in parallel since the BCEL library is not thread-safe
					classes = originalJar.stream()
						.filter(entry -> entry.getName().endsWith(".class"))
						.map(this::buildVerifiedClass)
						.filter(Optional::isPresent) // we only consider classes that did verify
						.map(Optional::get)
						.collect(Collectors.toCollection(TreeSet::new));
				}

				if (!hasErrors())
					// instrumentation and dump of the class files
					try (JarOutputStream instrumentedJar = this.instrumentedJar = new JarOutputStream(new FileOutputStream(destination.toFile()))) {
						// we cannot proceed in parallel since the BCEL library is not thread-safe
						classes.stream().forEach(this::dumpInstrumentedClass);
					}
			}
			catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}

		/**
		 * Yields a verified BCEL class from the given entry of the jar file.
		 * 
		 * @param entry the entry
		 * @return the BCEL class, if the class for {@code entry} did verify
		 */
		private Optional<VerifiedClass> buildVerifiedClass(JarEntry entry) {
			try (InputStream input = originalJar.getInputStream(entry)) {
				// generates a RAM image of the class file, by using the BCEL library for bytecode manipulation
				return Optional.of(new VerifiedClass(new ClassParser(input, entry.getName()).parse(), classLoader, issues::add, duringInitialization));
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			catch (VerificationException e) {
				return Optional.empty();
			}
		}

		/**
		 * Instruments the given class from a jar file.
		 * 
		 * @param clazz the class
		 */
		private void dumpInstrumentedClass(VerifiedClass clazz) {
			try {
				// add the same entry to the resulting jar
				instrumentedJar.putNextEntry(new JarEntry(clazz.getClassName().replace('.', '/') + ".class"));

				// dump an instrumented class file inside that entry
				new ClassInstrumentation(clazz, instrumentedJar);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}