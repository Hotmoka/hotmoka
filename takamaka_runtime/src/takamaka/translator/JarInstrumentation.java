package takamaka.translator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.bcel.classfile.ClassParser;

/**
 * An instrumenter of a jar file. It generates another jar files that
 * contain the same classes as the former, but instrumented. This means
 * that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public class JarInstrumentation {
	private static final Logger LOGGER = Logger.getLogger(JarInstrumentation.class.getName());

	/**
	 * Builds an instrumenter of the given jar file into another jar file.
	 * 
	 * @param origin the jar file to instrument
	 * @param destination the jar file to generate
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 */
	public JarInstrumentation(Path origin, Path destination, TakamakaClassLoader classLoader) throws IOException {
		new Initializer(origin, destination, classLoader);
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
		private final JarOutputStream instrumentedJar;

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

				try (JarFile originalJar = this.originalJar = new JarFile(origin.toFile())) {
					// we cannot proceed in parallel since the BCEL library is not thread-safe
					classes = originalJar.stream()
							.filter(entry -> entry.getName().endsWith(".class"))
							.map(this::parseClassEntry)
							.collect(Collectors.toCollection(TreeSet::new));
				}

				try (JarOutputStream instrumentedJar = this.instrumentedJar = new JarOutputStream(new FileOutputStream(destination.toFile()))) {
					// we cannot proceed in parallel since the BCEL library is not thread-safe
					classes.stream().forEach(this::addEntry);
				}
			}
			catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}

		/**
		 * Yields a BCEL class from the given entry of the jar file.
		 * 
		 * @param entry the entry
		 * @return the BCEL class
		 */
		private VerifiedClassGen parseClassEntry(JarEntry entry) {
			try (InputStream input = originalJar.getInputStream(entry)) {
				// generates a RAM image of the class file, by using the BCEL library for bytecode manipulation
				return new VerifiedClassGen(new ClassParser(input, entry.getName()).parse(), classLoader);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Instruments the given entry of a jar file, that is a class file.
		 * 
		 * @param entry the entry
		 */
		private void addEntry(VerifiedClassGen entry) {
			try {
				// add the same entry to the resulting jar
				instrumentedJar.putNextEntry(new JarEntry(entry.getClassName().replace('.', '/') + ".class"));

				// dump an instrumented class file inside that entry
				new ClassInstrumentation(entry, instrumentedJar, classLoader);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}