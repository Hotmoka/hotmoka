package io.takamaka.code.verification.internal;

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

import io.takamaka.code.verification.Annotations;
import io.takamaka.code.verification.BcelToClass;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.VerifiedJar;
import io.takamaka.code.verification.issues.Issue;

/**
 * An implementation of a jar that has undergone static verification before
 * installation into blockchain.
 */
public class VerifiedJarImpl implements VerifiedJar {

	/**
	 * The class loader used to load this jar.
	 */
	public final TakamakaClassLoader classLoader;

	/**
	 * The utility object that can be used to transform BCEL types into their corresponding
	 * Java class tag, by using the class loader of this jar.
	 */
	public final BcelToClass bcelToClass = new BcelToClassImpl(this);

	/**
	 * The utility that knows about the annotations of the methods in this jar.
	 */
	public final Annotations annotations = new AnnotationsImpl(this);

	/**
	 * The class of the jar that passed verification.
	 */
	private final SortedSet<VerifiedClass> classes = new TreeSet<>();

	/**
	 * The ordered set of errors and warnings generated while verifying the classes of the jar.
	 */
	private final SortedSet<Issue> issues = new TreeSet<>();

	/**
	 * Creates a verified jar from the given file. This verification
	 * might fail if at least a class did not verify. In that case, the issues generated
	 * during verification will contain at least an error.
	 * 
	 * @param origin the jar file to verify
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during
	 *                             blockchain initialization
	 * @throws IOException if there was a problem accessing the classes on disk
	 */
	public VerifiedJarImpl(Path origin, TakamakaClassLoader classLoader, boolean duringInitialization) throws IOException {
		this.classLoader = classLoader;

		new Initializer(origin, duringInitialization);
	}

	@Override
	public final boolean hasErrors() {
		return getFirstError().isPresent();
	}

	@Override
	public Optional<io.takamaka.code.verification.issues.Error> getFirstError() {
		return issues()
				.filter(issue -> issue instanceof io.takamaka.code.verification.issues.Error)
				.map(issue -> (io.takamaka.code.verification.issues.Error) issue)
				.findFirst();
	}

	@Override
	public Stream<VerifiedClass> classes() {
		return classes.stream();
	}

	@Override
	public Stream<Issue> issues() {
		return issues.stream();
	}

	@Override
	public TakamakaClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}

	@Override
	public BcelToClass getBcelToClass() {
		return bcelToClass;
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
		 * True if and only if the code instrumentation occurs during.
		 * blockchain initialization.
		 */

		private final boolean duringInitialization;

		/**
		 * Performs the instrumentation of the given jar file into another jar file.
		 * 
		 * @param origin the jar file to instrument
		 * @param duringInitialization true if and only if the instrumentation is performed during blockchain initialization
		 */
		private Initializer(Path origin, boolean duringInitialization) throws IOException {
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
				return Optional.of(VerifiedClass.of(new ClassParser(input, entry.getName()).parse(), VerifiedJarImpl.this, issues::add, duringInitialization));
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