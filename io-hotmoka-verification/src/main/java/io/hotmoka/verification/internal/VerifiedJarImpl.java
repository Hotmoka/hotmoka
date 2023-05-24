/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.verification.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.util.ClassLoaderRepository;

import io.hotmoka.verification.UnsupportedVerificationVersionException;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.api.Annotations;
import io.hotmoka.verification.api.BcelToClass;
import io.hotmoka.verification.api.Error;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.VerifiedClass;
import io.hotmoka.verification.api.VerifiedJar;

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
	 * The ordered set of errors generated while verifying the classes of the jar.
	 */
	private final SortedSet<io.hotmoka.verification.api.Error> errors = new TreeSet<>();

	/**
	 * Creates a verified jar from the given file. This verification
	 * might fail if at least a class did not verify. In that case, the errors generated
	 * during verification will contain at least an error.
	 * 
	 * @param origin the jar file to verify, given as an array of bytes
	 * @param classLoader the class loader that can be used to resolve the classes of the program, including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during the node initialization
	 * @param allowSelfCharged true if and only if {@code @@SelfCharged} methods are allowed
	 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
	 * @throws IOException if there was a problem accessing the classes on disk
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 * @throws UnsupportedVerificationVersionException if the verification version is not available
	 */
	public VerifiedJarImpl(byte[] origin, TakamakaClassLoader classLoader, boolean duringInitialization, boolean allowSelfCharged, boolean skipsVerification) throws IOException, ClassNotFoundException, UnsupportedVerificationVersionException {
		this.classLoader = classLoader;

		// we set the BCEL repository so that it matches the class path made up of the jar to
		// instrument and its dependencies. This is important since class instrumentation will use
		// the repository to infer least common supertypes during type inference, hence the
		// whole hierarchy of classes must be available to BCEL through its repository
		Repository.setRepository(new ClassLoaderRepository(classLoader.getJavaClassLoader()));

		new Initializer(origin, duringInitialization, allowSelfCharged, skipsVerification);
	}

	@Override
	public final boolean hasErrors() {
		return !errors.isEmpty();
	}

	@Override
	public void forEachError(Consumer<Error> action) {
		errors.forEach(action);
	}

	@Override
	public Optional<io.hotmoka.verification.api.Error> getFirstError() {
		try {
			return Optional.of(errors.first());
		}
		catch (NoSuchElementException e) {
			return Optional.empty();
		}
	}

	@Override
	public Stream<VerifiedClass> classes() {
		return classes.stream();
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
		 * True if and only if {@code @@SelfCharged} methods are allowed.
		 */
		private final boolean allowSelfCharged;

		/**
		 * True if and only if the static verification of the classses in the jar must be skipped.
		 */
		private final boolean skipsVerification;

		/**
		 * The manager of the versions of the verification module.
		 */
		private final VersionsManager versionsManager;

		/**
		 * Performs the verification of the given jar file into another jar file.
		 * 
		 * @param origin the jar file to verify, as an array of bytes
		 * @param duringInitialization true if and only if the verification is performed during the initialization of the node
		 * @param allowSelfCharged true if and only if {@code @@SelfCharged} methods are allowed
		 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
		 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
		 * @throws UnsupportedVerificationVersionException if the verification version is not available
		 */
		private Initializer(byte[] origin, boolean duringInitialization, boolean allowSelfCharged, boolean skipsVerification) throws IOException, ClassNotFoundException, UnsupportedVerificationVersionException {
			this.duringInitialization = duringInitialization;
			this.allowSelfCharged = allowSelfCharged;
			this.versionsManager = new VersionsManager(classLoader.getVerificationVersion());
			this.skipsVerification = skipsVerification;

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
		 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
		 */
		private Optional<VerifiedClass> buildVerifiedClass(ZipEntry entry, InputStream input) throws ClassNotFoundException {
			try {
				// generates a RAM image of the class file, by using the BCEL library for bytecode manipulation
				return Optional.of(new VerifiedClassImpl(new ClassParser(input, entry.getName()).parse(), VerifiedJarImpl.this, versionsManager, errors::add, duringInitialization, allowSelfCharged, skipsVerification));
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