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
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassLoaderRepository;

import io.hotmoka.verification.api.Error;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerificationException;
import io.hotmoka.verification.api.VerifiedClass;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * An implementation of a jar that has undergone static verification before
 * installation into blockchain.
 */
public class VerifiedJarImpl implements VerifiedJar {

	/**
	 * The class loader used to load this jar.
	 */
	private final TakamakaClassLoader classLoader;

	/**
	 * The class of the jar that passed verification.
	 */
	private final SortedSet<VerifiedClass> classes = new TreeSet<>();

	private final static Logger LOGGER = Logger.getLogger(VerifiedJarImpl.class.getName());

	/**
	 * Creates a verified jar from the given file. Calls the given task for each error
	 * generated during the verification. At the end, throws an exception if there is at least an error.
	 * 
	 * @param jar the jar file to verify, given as an array of bytes
	 * @param classLoader the class loader that can be used to resolve the classes of the program, including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during the node initialization
	 * @param onError a task to execute for each error found during the verification
	 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
	 * @throws VerificationException if verification fails
	 * @throws IllegalJarException if the jar under verification is illegal
	 * @throws UnknownTypeException if some type of the jar under verification cannot be resolved
	 */
	public VerifiedJarImpl(byte[] jar, TakamakaClassLoader classLoader, boolean duringInitialization, Consumer<Error> onError, boolean skipsVerification) throws VerificationException, IllegalJarException, UnknownTypeException {
		this.classLoader = classLoader;

		// we set the BCEL repository so that it matches the class path made up of the jar to
		// instrument and its dependencies. This is important since class instrumentation will use
		// the repository to infer least common supertypes during type inference, hence the
		// whole hierarchy of classes must be available to BCEL through its repository
		Repository.setRepository(new ClassLoaderRepository(classLoader.getJavaClassLoader()));

		SortedSet<io.hotmoka.verification.api.Error> errors = new TreeSet<>();
		new Initializer(jar, duringInitialization, skipsVerification, errors);

		errors.forEach(onError);

		if (!errors.isEmpty())
			throw new VerificationException(errors.getFirst().getMessage());
	}

	@Override
	public Stream<VerifiedClass> getClasses() {
		return classes.stream();
	}

	@Override
	public TakamakaClassLoader getClassLoader() {
		return classLoader;
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
		 * True if and only if the static verification of the classes in the jar must be skipped.
		 */
		private final boolean skipsVerification;

		/**
		 * The manager of the versions of the verification module.
		 */
		private final VersionsManager versionsManager;

		/**
		 * Performs the verification of the given jar file.
		 * 
		 * @param origin the jar file to verify, as an array of bytes
		 * @param duringInitialization true if and only if the verification is performed during the initialization of the node
		 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
		 * @param errors the container where new errors must be added
		 * @throws IllegalJarException if the jar under verification is illegal
		 * @throws UnknownTypeException if some type of the jar under verification cannot be resolved
		 */
		private Initializer(byte[] origin, boolean duringInitialization, boolean skipsVerification, SortedSet<io.hotmoka.verification.api.Error> errors) throws IllegalJarException, UnknownTypeException {
			this.duringInitialization = duringInitialization;

			try {
				this.versionsManager = new VersionsManager(classLoader.getVerificationVersion());
			}
			catch (UnsupportedVerificationVersionException e) {
				// the class loader was already constructed with this verification version, hence
				// it must be available, or otherwise there is a programming bug
				throw new RuntimeException(e);
			}

			this.skipsVerification = skipsVerification;

			// parsing and verification of the class files
			try (var zis = new ZipInputStream(new ByteArrayInputStream(origin))) {
				// we cannot proceed in parallel since the BCEL library is not thread-safe
				ZipEntry entry;
    			while ((entry = zis.getNextEntry()) != null) {
    				String name = entry.getName();
    				if (name.endsWith(".class") && !"module-info.class".equals(name))
    					buildVerifiedClass(entry, zis, errors).ifPresent(classes::add);
    			}
			}
			catch (IOException e) {
				LOGGER.log(Level.WARNING, "cannot unzip the jar file", e);
				throw new IllegalJarException("Cannot unzip the jar file");
			}
		}

		/**
		 * Yields a verified BCEL class from the given entry of the jar file.
		 * If verification fails for that class, an empty result is returned
		 * and the verification errors are accumulated inside the verified jar.
		 * 
		 * @param entry the entry
		 * @param input the stream of the jar in the entry
		 * @param errors a container where new errors must be added
		 * @return the BCEL class, if the class for {@code entry} did verify
		 * @throws IllegalJarException if the jar under verification is illegal
		 * @throws UnknownTypeException if some type of the jar under verification cannot be resolved
		 */
		private Optional<VerifiedClass> buildVerifiedClass(ZipEntry entry, InputStream input, SortedSet<io.hotmoka.verification.api.Error> errors) throws IllegalJarException, UnknownTypeException {
			JavaClass parsedClass;

			try {
				parsedClass = new ClassParser(input, entry.getName()).parse();
			}
			catch (ClassFormatException | IOException e) {
				LOGGER.log(Level.WARNING, "cannot parse " + entry.getName(), e);
				throw new IllegalJarException("Cannot parse " + entry.getName());
			}

			try {
				return Optional.of(new VerifiedClassImpl(parsedClass, VerifiedJarImpl.this, versionsManager, errors::add, duringInitialization, skipsVerification));
			}
			catch (VerificationException e) {
				return Optional.empty();
			}
		}
	}
}