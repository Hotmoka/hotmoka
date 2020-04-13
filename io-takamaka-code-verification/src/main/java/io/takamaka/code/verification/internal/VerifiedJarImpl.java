package io.takamaka.code.verification.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.util.ClassLoaderRepository;

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
	public final BcelToClassImpl bcelToClass = new BcelToClassImpl(this);

	/**
	 * The utility that knows about the annotations of the methods in this jar.
	 */
	public final AnnotationsImpl annotations = new AnnotationsImpl(this);

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
	 * @param origin the jar file to verify, given as an array of bytes
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during
	 *                             blockchain initialization
	 * @throws IOException if there was a problem accessing the classes on disk
	 */
	public VerifiedJarImpl(byte[] origin, TakamakaClassLoader classLoader, boolean duringInitialization) throws IOException {
		this.classLoader = classLoader;

		// we set the BCEL repository so that it matches the class path made up of the jar to
		// instrument and its dependencies. This is important since class instrumentation will use
		// the repository to infer least common supertypes during type inference, hence the
		// whole hierarchy of classes must be available to BCEL through its repository
		Repository.setRepository(new ClassLoaderRepository(classLoader.getJavaClassLoader()));

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
		 * True if and only if the code instrumentation occurs during.
		 * blockchain initialization.
		 */

		private final boolean duringInitialization;

		/**
		 * Performs the verification of the given jar file into another jar file.
		 * 
		 * @param origin the jar file to verify, as an array of bytes
		 * @param duringInitialization true if and only if the verification is performed during blockchain initialization
		 */
		private Initializer(byte[] origin, boolean duringInitialization) throws IOException {
			this.duringInitialization = duringInitialization;

			// parsing and verification of the class files
			try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(origin))) {
				// we cannot proceed in parallel since the BCEL library is not thread-safe
				ZipEntry entry;
    			while ((entry = zis.getNextEntry()) != null)
    				if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class"))
    					buildVerifiedClass(entry, zis).ifPresent(classes::add);
			}
			catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}

		/**
		 * Yields a verified BCEL class from the given entry of the jar file.
		 * 
		 * @param entry the entry
		 * @param input the stream of the jar in the entry
		 * @return the BCEL class, if the class for {@code entry} did verify
		 */
		private Optional<VerifiedClass> buildVerifiedClass(ZipEntry entry, InputStream input) {
			try {
				// generates a RAM image of the class file, by using the BCEL library for bytecode manipulation
				return Optional.of(new VerifiedClassImpl(new ClassParser(input, entry.getName()).parse(), VerifiedJarImpl.this, issues::add, duringInitialization));
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