package takamaka.translator;

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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.classfile.ClassParser;

import takamaka.verifier.Issue;
import takamaka.verifier.VerificationException;
import takamaka.verifier.VerifiedClassGen;

/**
 * An instrumenter of a jar file. It generates another jar files that
 * contain the same classes as the former, but instrumented. This means
 * that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public class JarInstrumentation {
	private static final Logger LOGGER = Logger.getLogger(JarInstrumentation.class.getName());

	/**
	 * The errors and warnings generated while verifying the classes of the jar.
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
	 * @throws IOException if there was a problem accessing the classes on disk
	 */
	public JarInstrumentation(Path origin, Path destination, TakamakaClassLoader classLoader) throws IOException {
		new Initializer(origin, destination, classLoader);
	}

	/**
	 * Determines if verification of at least a class of the jar failed with an error.
	 * 
	 * @return true if and only if that condition holds
	 */
	public boolean verificationFailed() {
		return issues().anyMatch(issue -> issue instanceof takamaka.verifier.Error);
	}

	/**
	 * Yields the first error (hence not a warning) that occurred during verification of this jar.
	 */
	public Optional<takamaka.verifier.Error> getFirstError() {
		return issues()
			.filter(issue -> issue instanceof takamaka.verifier.Error)
			.map(issue -> (takamaka.verifier.Error) issue)
			.findFirst();
	}

	/**
	 * Yields the issues generated during verification of the classes of the origin jar.
	 * 
	 * @return the issues
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
		 * Performs the instrumentation of the given jar file into another jar file.
		 * 
		 * @param origin the jar file to instrument
		 * @param destination the jar file to generate
		 * @param classLoader the class loader that can be used to resolve the classes of the program,
		 *                    including those of {@code origin}
		 */
		private Initializer(Path origin, Path destination, TakamakaClassLoader classLoader) throws IOException {
			LOGGER.fine(() -> "Processing " + origin);

			this.classLoader = classLoader;

			try {
				SortedSet<VerifiedClassGen> classes;

				// parsing and verification of the class files
				try (JarFile originalJar = this.originalJar = new JarFile(origin.toFile())) {
					// we cannot proceed in parallel since the BCEL library is not thread-safe
					classes = originalJar.stream()
						.filter(entry -> entry.getName().endsWith(".class"))
						.map(this::buildVerifiedClass)
						.filter(clazz -> clazz != null) // we do not collect null's standing for classes that do not verify
						.collect(Collectors.toCollection(TreeSet::new));
				}

				if (!verificationFailed())
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
		 * @return the BCEL class, or {@code null} if the class for {@code entry} did not verify
		 */
		private VerifiedClassGen buildVerifiedClass(JarEntry entry) {
			try (InputStream input = originalJar.getInputStream(entry)) {
				// generates a RAM image of the class file, by using the BCEL library for bytecode manipulation
				return new VerifiedClassGen(new ClassParser(input, entry.getName()).parse(), classLoader, issues::add);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			catch (VerificationException e) {
				return null;
			}
		}

		/**
		 * Instruments the given class from a jar file.
		 * 
		 * @param clazz the class
		 */
		private void dumpInstrumentedClass(VerifiedClassGen clazz) {
			try {
				// add the same entry to the resulting jar
				instrumentedJar.putNextEntry(new JarEntry(clazz.getClassName().replace('.', '/') + ".class"));

				// dump an instrumented class file inside that entry
				new ClassInstrumentation(clazz, instrumentedJar, classLoader);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}