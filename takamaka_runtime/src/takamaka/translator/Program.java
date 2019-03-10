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

public class Program {
	private static final Logger LOGGER = Logger.getLogger(Program.class.getName());
	private final ConcurrentMap<String, JavaClass> classes = new ConcurrentHashMap<>();

	public Program(Stream<Path> jars) throws IOException {
		LOGGER.fine("Building program");

		try {
			jars.forEach(this::processJar);
		}
		catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public JavaClass get(String className) {
		return classes.get(className);
	}

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

	private void addClass(JarEntry entry, JarFile jar) {
		try (final InputStream input = jar.getInputStream(entry)) {
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

	private static boolean isClass(JarEntry entry) {
		return entry.getName().endsWith(".class");
	}
}