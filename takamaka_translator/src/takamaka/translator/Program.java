package takamaka.translator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class Program {
	private final ConcurrentMap<String, JavaClass> classes = new ConcurrentHashMap<>();

	public Program(Stream<JarFile> jars) {
		jars.forEach(this::processJar);;
	}

	public JavaClass get(String className) {
		return classes.get(className);
	}

	private void processJar(JarFile jar) {
		jar.stream()
			.parallel()
			.filter(Program::isClass)
			.forEach(entry -> addClass(entry, jar));
	}

	private void addClass(JarEntry entry, JarFile jar) {
		try (final InputStream input = jar.getInputStream(entry)) {
			String entryName = entry.getName();
			JavaClass clazz = new ClassParser(input, entryName).parse();
			if (clazz != classes.putIfAbsent(clazz.getClassName(), clazz))
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