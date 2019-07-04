package takamaka.translator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

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
	 * @param program the program that contains all classes of the program,
	 *                including those of {@code origin}
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 */
	public JarInstrumentation(Path origin, Path destination, Program program, ClassLoader classLoader) {
		new Initializer(origin, destination, program, classLoader);
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
		 * The program that contains all classes.
		 */
		private final Program program;

		/**
		 * The class loader that can be used to resolve the classes of the program.
		 */
		private final ClassLoader classLoader;

		/**
		 * Performs the instrumentation of the given jar file into another jar file.
		 * 
		 * @param origin the jar file to instrument
		 * @param destination the jar file to generate
		 * @param program the program that contains all classes of the program,
		 *                including those of {@code origin}
		 * @param classLoader the class loader that can be used to resolve the classes of the program,
		 *                    including those of {@code origin}
		 */
		private Initializer(Path origin, Path destination, Program program, ClassLoader classLoader) {
			LOGGER.fine(() -> "Processing " + origin);

			this.program = program;
			this.classLoader = classLoader;

			try (JarFile originalJar = this.originalJar = new JarFile(origin.toFile());
				 JarOutputStream instrumentedJar = this.instrumentedJar = new JarOutputStream(new FileOutputStream(destination.toFile()))) {

				// we cannot proceed in parallel since the BCEL library is not thread-safe
				originalJar.stream().forEach(this::addEntry);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Instruments the given entry of a jar file, if it is a class file.
		 * 
		 * @param entry the entry
		 */
		private void addEntry(JarEntry entry) {
			try (InputStream input = originalJar.getInputStream(entry)) {
				String entryName = entry.getName();

				if (entryName.endsWith(".class")) {
					// add the same entry to the resulting jar
					instrumentedJar.putNextEntry(new JarEntry(entryName));

					// dump an instrumented class file inside that entry
					new ClassInstrumentation(input, entryName, instrumentedJar, program, classLoader);
				}
				else
					LOGGER.fine(() -> "Dropping non-class file " + entryName);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}