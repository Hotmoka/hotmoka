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

import static io.hotmoka.exceptions.CheckRunnable.check2;
import static io.hotmoka.exceptions.UncheckConsumer.uncheck;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
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
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.api.Bootstraps;
import io.hotmoka.verification.api.Pushers;
import io.hotmoka.verification.api.VerifiedClass;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.whitelisting.api.WhiteListingProofObligation;

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
	public final VerifiedJarImpl jar;

	/**
	 * The utility object that knows about the lambda bootstraps contained in this class.
	 */
	public final BootstrapsImpl bootstraps;

	/**
	 * The utility object that allows one to follow the stack pushers of values in the stack
	 * of the code of this class.
	 */
	public final PushersImpl pushers;

	/**
	 * The utility that can be used to resolve targets of calls and field accesses in this class.
	 */
	public final Resolver resolver;

	/**
	 * Builds and verifies a class from the given class file.
	 * 
	 * @param clazz the parsed class file
	 * @param jar the jar this class belongs to
	 * @param versionsManager the manager of the versions of the verification module
	 * @param issueHandler the handler that is notified of every verification error or warning
	 * @param duringInitialization true if and only if the class is verified during the initialization of the node
	 * @param allowSelfCharged true if and only if {@code @@SelfCharged} methods are allowed
	 * @param skipsVerification true if and only if the static verification of the class must be skipped
	 * @throws VerificationException if the class could not be verified
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	VerifiedClassImpl(JavaClass clazz, VerifiedJarImpl jar, VersionsManager versionsManager, Consumer<AbstractErrorImpl> issueHandler, boolean duringInitialization, boolean allowSelfCharged, boolean skipsVerification) throws VerificationException, ClassNotFoundException {
		this.clazz = new ClassGen(clazz);
		this.jar = jar;
		ConstantPoolGen cpg = getConstantPool();
		String className = getClassName();
		MethodGen[] methods = Stream.of(clazz.getMethods()).map(method -> new MethodGen(method, className, cpg)).toArray(MethodGen[]::new);
		this.bootstraps = new BootstrapsImpl(this, methods);
		this.pushers = new PushersImpl(this);
		this.resolver = new Resolver(this);

		if (!skipsVerification)
			new Verification(issueHandler, methods, duringInitialization, allowSelfCharged, versionsManager);
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
	public Field whiteListingModelOf(FieldInstruction fi) throws ClassNotFoundException {
		return jar.classLoader.getWhiteListingWizard().whiteListingModelOf(resolver.resolvedFieldFor(fi).get()).get();
	}

	@Override
	public Executable whiteListingModelOf(InvokeInstruction invoke) throws ClassNotFoundException {
		return whiteListingModelOf(resolver.resolvedExecutableFor(invoke).get(), invoke).get();
	}

	@Override
	public VerifiedJar getJar() {
		return jar;
	}

	@Override
	public Bootstraps getBootstraps() {
		return new BootstrapsImpl(bootstraps);
	}

	@Override
	public Pushers getPushers() {
		return pushers;
	}

	@Override
	public JavaClass toJavaClass() {
		return clazz.getJavaClass();
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
	 * class, if it belongs to some Java run-time support class. If the instruction is a special call
	 * to a method of a superclass, it checks that white-listing annotations on the receiver are not fooled.
	 * 
	 * @param executable the method or constructor whose model is looked for
	 * @param invoke the call to the method or constructor
	 * @return the model of its white-listing, if it exists
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	Optional<? extends Executable> whiteListingModelOf(Executable executable, InvokeInstruction invoke) throws ClassNotFoundException {
		if (executable instanceof Constructor<?>)
			return checkINVOKESPECIAL(invoke, jar.classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable));
		else
			return checkINVOKESPECIAL(invoke, jar.classLoader.getWhiteListingWizard().whiteListingModelOf((Method) executable));
	}

	/**
	 * If the given invoke instruction is an {@code invokespecial} and the given model
	 * has a white-listing annotation on its receiver, checks if the model
	 * of the resolved target of the invoke is still in the code installed in blockchain.
	 * This check is important in order to forbid calls such as super.hashCode() to the hashCode()
	 * method of Object, that would be non-deterministic.
	 * 
	 * @param invoke the invoke instruction
	 * @param model the white-listing model of the invoke
	 * @return the optional containing the model, or the empty optional if the check fails
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	private Optional<? extends Executable> checkINVOKESPECIAL(InvokeInstruction invoke, Optional<? extends Executable> model) throws ClassNotFoundException {
		if (invoke instanceof INVOKESPECIAL &&
			model.isPresent() &&
			hasWhiteListingProofObligationOnReceiver(model.get()) &&
			resolver.resolvedExecutableFor(invoke).get().getDeclaringClass() == Object.class)
			return Optional.empty();
		else
			return model;
	}

	/**
	 * Determines if the given method or constructor has a white-listing proof obligation
	 * on its receiver.
	 * 
	 * @param executable the method or constructor
	 * @return true if and only if that condition holds
	 */
	private static boolean hasWhiteListingProofObligationOnReceiver(Executable executable) {
		return Stream.of(executable.getAnnotations())
			.anyMatch(annotation -> annotation.annotationType().getAnnotation(WhiteListingProofObligation.class) != null);
	}

	/**
	 * The algorithms that perform the verification of the BCEL class. It is passed
	 * as context to each single verification step.
	 */
	public class Verification {

		/**
		 * The manager of the versions of the verification module.
		 */
		final VersionsManager versionsManager;

		/**
		 * The handler that must be notified of issues found in the class.
		 */
		final Consumer<AbstractErrorImpl> issueHandler;

		/**
		 * True if and only if the code verification occurs during blockchain initialization.
		 */
		final boolean duringInitialization;

		/**
		 * True if and only if {@code @SelfCharged} methods are allowed.
		 */
		final boolean allowSelfCharged;

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
		 * @param duringInitialization true if and only if verification is performed during the initialization of the node
		 * @param allowSelfCharged true if and only if {@code @@SelfCharged} methods are allowed
		 * @param versionsManager the manager of the versions of the verification module
		 * @throws VerificationException if some verification error occurs
		 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
		 */
		private Verification(Consumer<AbstractErrorImpl> issueHandler, MethodGen[] methods, boolean duringInitialization, boolean allowSelfCharged, VersionsManager versionsManager) throws VerificationException, ClassNotFoundException {
			this.issueHandler = issueHandler;
			this.versionsManager = versionsManager;
			ConstantPoolGen cpg = getConstantPool();
			this.methods = methods;
			this.lines = Stream.of(methods).collect(Collectors.toMap(method -> method, method -> method.getLineNumberTable(cpg)));
			this.duringInitialization = duringInitialization;
			this.allowSelfCharged = allowSelfCharged;

			applyAllChecksToTheClass();
			applyAllChecksToTheMethodsOfTheClass();

			if (hasErrors)
				throw new VerificationException();
		}

		private void applyAllChecksToTheClass() throws ClassNotFoundException {
			versionsManager.applyAllClassChecks(this);
		}

		private void applyAllChecksToTheMethodsOfTheClass() throws ClassNotFoundException {
			check2(ClassNotFoundException.class, () -> Stream.of(methods).forEachOrdered(uncheck(method -> versionsManager.applyAllMethodChecks(this, method))));
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