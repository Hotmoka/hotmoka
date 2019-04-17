package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

/**
 * A collection of Java bytecode classes (class files)
 * from one or more jars. This container is thread-safe.
 */
public class Program {
	private static final Logger LOGGER = Logger.getLogger(Program.class.getName());

	/**
	 * A map from each class name to its Java bytecode.
	 */
	private final ConcurrentMap<String, JavaClass> classes = new ConcurrentHashMap<>();

	/**
	 * Builds a program for the given jars.
	 * 
	 * @param jars the jars
	 * @throws IOException if some jar cannot be accessed
	 */
	public Program(Stream<Path> jars) throws IOException {
		LOGGER.fine("Building program");

		try {
			jars.forEach(this::processJar);
		}
		catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	/**
	 * Yields the Java bytecode of the class with the given name.
	 * 
	 * @param className the name of the class
	 * @return the Java bytecode, if any. Yields {@code null} otherwise
	 */
	public JavaClass get(String className) {
		return classes.get(className);
	}

	/**
	 * Parses the given jar, by extracting the class files
	 * contained therein.
	 * 
	 * @param jar the jar to parse
	 */
	private void processJar(Path jar) {
		try (JarFile jarFile = new JarFile(jar.toFile())) {
			jarFile.stream()
				.parallel()
				.filter(Program::isClass)
				.forEach(entry -> addClass(entry, jarFile));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Adds the given class file to this program.
	 * 
	 * @param entry the entry corresponding to the class file
	 * @param jar the jar where the class file occurs
	 */
	private void addClass(JarEntry entry, JarFile jar) {
		try (InputStream input = jar.getInputStream(entry)) {
			String entryName = entry.getName();
			JavaClass clazz = new ClassParser(input, entryName).parse();
			JavaClass added = classes.putIfAbsent(clazz.getClassName(), clazz);
			if (added != null && added != clazz)
				throw new IllegalArgumentException("Class " + clazz.getClassName() + " is redefined in the jars");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Checks if the given jar entry seems to contain a class file.
	 * 
	 * @param entry the entry
	 * @return true if that condition holds
	 */
	private static boolean isClass(JarEntry entry) {
		return entry.getName().endsWith(".class");
	}
}