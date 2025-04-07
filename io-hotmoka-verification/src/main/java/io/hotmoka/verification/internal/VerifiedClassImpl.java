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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.api.Bootstraps;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerificationException;
import io.hotmoka.verification.api.VerifiedClass;
import io.hotmoka.verification.api.VerifiedJar;

/**
 * A class that passed the static Takamaka verification tests.
 */
public class VerifiedClassImpl implements VerifiedClass {

	/**
	 * The class generator used to generate this object.
	 */
	private final ClassGen clazz;

	/**
	 * The jar this class belongs to.
	 */
	private final VerifiedJarImpl jar;

	/**
	 * The utility object that knows about the lambda bootstraps contained in this class.
	 */
	private final BootstrapsImpl bootstraps;

	/**
	 * The utility that can be used to resolve targets of calls and field accesses in this class.
	 */
	private final Resolver resolver;

	/**
	 * Builds and verifies a class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param jar the jar where this class will be added at the end
	 * @param versionsManager the manager of the versions of the verification module
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is verified during the initialization of the node
	 * @param skipsVerification true if and only if the static verification of the class must be skipped
	 * @throws IllegalJarException if {@code jar} is illegal, because incomplete or containing an illegal bytecode
	 * @throws VerificationException if the Takamaka verification of the class failed
	 * @throws UnknownTypeException if some class of the Takamaka program cannot be loaded
	 */
	public VerifiedClassImpl(JavaClass clazz, VerifiedJarImpl jar, VersionsManager versionsManager, Consumer<AbstractError> issueHandler, boolean duringInitialization, boolean skipsVerification) throws VerificationException, IllegalJarException, UnknownTypeException {
		this.clazz = new ClassGen(clazz);
		this.jar = jar;
		ConstantPoolGen cpg = getConstantPool();
		String className = getClassName();
		var methods = Stream.of(clazz.getMethods()).map(method -> new MethodGen(method, className, cpg)).toArray(MethodGen[]::new);
		this.bootstraps = new BootstrapsImpl(this, methods);
		this.resolver = new Resolver(this);

		if (!skipsVerification)
			new Verification(issueHandler, methods, duringInitialization, versionsManager);
	}

	@Override
	public String getClassName() {
		return clazz.getClassName();
	}

	@Override
	public int compareTo(VerifiedClass other) {
		return getClassName().compareTo(other.getClassName());
	}

	@Override
	public VerifiedJar getJar() {
		return jar;
	}

	@Override
	public Bootstraps getBootstraps() {
		return bootstraps;
	}

	@Override
	public JavaClass toJavaClass() {
		return clazz.getJavaClass();
	}

	/**
	 * Yields the utility that can be used to resolve targets of calls and field accesses in this class.
	 * 
	 * @return the utility
	 */
	public Resolver getResolver() {
		return resolver;
	}

	/**
	 * Yields the constant pool of this class.
	 * 
	 * @return the constant pool
	 */
	ConstantPoolGen getConstantPool() {
		return clazz.getConstantPool();
	}

	/**
	 * Yields the attributes of this class.
	 * 
	 * @return the attributes
	 */
	Attribute[] getAttributes() {
		return clazz.getAttributes();
	}

	/**
	 * Looks for a white-listing model of the given method or constructor. That is a constructor declaration
	 * that justifies why the method or constructor is white-listed. It can be the method or constructor itself, if it
	 * belongs to a class installed in blockchain, or otherwise a method or constructor of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param executable the method or constructor whose model is looked for
	 * @param invoke the call to the method or constructor
	 * @return the model of its white-listing, if it exists
	 */
	Optional<? extends Executable> whiteListingModelOf(Executable executable, InvokeInstruction invoke) {
		if (executable instanceof Constructor<?>)
			return jar.getClassLoader().getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable);
		else
			return jar.getClassLoader().getWhiteListingWizard().whiteListingModelOf((Method) executable);
	}

	/**
	 * The algorithms that perform the verification of the BCEL class. It is passed
	 * as context to each single verification step.
	 */
	public class Verification {

		/**
		 * The handler that must be notified of issues found in the class.
		 */
		final Consumer<AbstractError> issueHandler;

		/**
		 * True if and only if the code verification occurs during blockchain initialization.
		 */
		final boolean duringInitialization;

		/**
		 * A map from each method to its line number table.
		 */
		private final Map<MethodGen, LineNumberTable> lines;

		/**
		 * The methods of the class under verification.
		 */
		private final MethodGen[] methods;

		/**
		 * True if and only if at least an error was issued during verification.
		 */
		private boolean hasErrors;

		/**
		 * Performs the static verification of this class.
		 * 
		 * @param issueHandler the handler to call when an issue is found
		 * @param methods the methods of the class
		 * @param duringInitialization true if and only if verification is performed during the initialization of the Hotmoka node
		 * @param versionsManager the manager of the versions of the verification module
		 * @throws VerificationException if some verification error occurs
		 * @throws IllegalJarException if the jar under verification is illegal
		 * @throws UnknownTypeException if some type of the jar under verification cannot be resolved
		 */
		private Verification(Consumer<AbstractError> issueHandler, MethodGen[] methods, boolean duringInitialization, VersionsManager versionsManager) throws VerificationException, IllegalJarException, UnknownTypeException {
			this.issueHandler = issueHandler;
			ConstantPoolGen cpg = getConstantPool();
			this.methods = methods;
			this.lines = Stream.of(methods).collect(Collectors.toMap(method -> method, method -> method.getLineNumberTable(cpg)));
			this.duringInitialization = duringInitialization;

			applyAllChecksToTheClass(versionsManager);
			applyAllChecksToTheMethodsOfTheClass(versionsManager);

			if (hasErrors)
				throw new VerificationException("Class verification failed");
		}

		private void applyAllChecksToTheClass(VersionsManager versionsManager) throws IllegalJarException, UnknownTypeException {
			versionsManager.applyAllClassChecks(this);
		}

		private void applyAllChecksToTheMethodsOfTheClass(VersionsManager versionsManager) throws IllegalJarException, UnknownTypeException {
			for (var method: methods)
				versionsManager.applyAllMethodChecks(this, method);
		}

		/**
		 * Yields the class that is being verified.
		 * 
		 * @return the class
		 */
		final VerifiedClassImpl getVerifiedClass() {
			return VerifiedClassImpl.this;
		}

		/**
		 * Yields the generator of the class being verified.
		 * 
		 * @return the generator of the class
		 */
		final ClassGen getClassGen() {
			return clazz;
		}

		final void setHasErrors() {
			hasErrors = true;
		}

		final Stream<MethodGen> getMethods() {
			return Stream.of(methods);
		}

		final LineNumberTable getLinesFor(MethodGen method) {
			return lines.get(method);
		}
	}
}