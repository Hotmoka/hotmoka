package io.takamaka.code.verification;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.bcel.classfile.ClassParser;

import io.takamaka.code.verification.issues.Issue;

public class VerifiedJar {

	/**
	 * The class of the jar that passed verification.
	 */
	private final SortedSet<VerifiedClass> classes = new TreeSet<>();

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
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 * @param duringInitialization true if and only if the code instrumentation occurs during
	 *                             blockchain initialization
	 * @throws IOException if there was a problem accessing the classes on disk
	 */
	public VerifiedJar(Path origin, TakamakaClassLoader classLoader, boolean duringInitialization) throws IOException {
		new Initializer(origin, classLoader, duringInitialization);
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
	public Optional<io.takamaka.code.verification.issues.Error> getFirstError() {
		return issues()
				.filter(issue -> issue instanceof io.takamaka.code.verification.issues.Error)
				.map(issue -> (io.takamaka.code.verification.issues.Error) issue)
				.findFirst();
	}

	/**
	 * Yields the stream of the classes of the jar that passed verification.
	 * 
	 * @return the classes, in increasing order
	 */
	public Stream<VerifiedClass> classes() {
		return classes.stream();
	}

	/**
	 * Yields the issues generated during the verification of the classes of the jar.
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
		 * @param classLoader the class loader that can be used to resolve the classes of the program,
		 *                    including those of {@code origin}
		 * @param duringInitialization true if and only if the instrumentation is performed during blockchain initialization
		 */
		private Initializer(Path origin, TakamakaClassLoader classLoader, boolean duringInitialization) throws IOException {
			this.classLoader = classLoader;
			this.duringInitialization = duringInitialization;

			// parsing and verification of the class files
			try (JarFile originalJar = this.originalJar = new JarFile(origin.toFile())) {
				// we cannot proceed in parallel since the BCEL library is not thread-safe
				originalJar.stream()
				.filter(entry -> entry.getName().endsWith(".class"))
				.map(this::buildVerifiedClass)
				.filter(Optional::isPresent) // we only consider classes that did verify
				.map(Optional::get)
				.forEach(classes::add);
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
	}
}