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

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.AnnotationUtilities;
import io.hotmoka.verification.BcelToClassTransformers;
import io.hotmoka.verification.api.AnnotationUtility;
import io.hotmoka.verification.api.BcelToClassTransformer;
import io.hotmoka.verification.api.Bootstraps;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * A verification check on a class.
 */
public abstract class CheckOnClasses {
	private final VerifiedClassImpl.Verification builder;
	protected final TakamakaClassLoader classLoader;
	protected final Class<?> clazz;
	protected final Bootstraps bootstraps;
	protected final Resolver resolver;
	protected final AnnotationUtility annotations;
	protected final BcelToClassTransformer bcelToClass;
	protected final boolean duringInitialization;
	protected final String className;
	protected final ConstantPoolGen cpg;
	protected final boolean isContract;
	protected final boolean isStorage;
	protected final boolean isInterface;
	protected final boolean isWhiteListedDuringInitialization;

	protected CheckOnClasses(VerifiedClassImpl.Verification builder) throws IllegalJarException {
		this.builder = builder;
		VerifiedClassImpl verifiedClass = builder.getVerifiedClass();
		this.classLoader = verifiedClass.getJar().getClassLoader();
		this.className = verifiedClass.getClassName();

		try {
			this.clazz = classLoader.loadClass(className);
			this.isInterface = classLoader.isInterface(className);
		}
		catch (ClassNotFoundException e) {
			// the class was loaded with this class loader, therefore this exception should never occur
			throw new RuntimeException(e);
		}

		this.annotations = AnnotationUtilities.of(verifiedClass.getJar());

		try {
			this.isContract = classLoader.isContract(className);
			this.isStorage = classLoader.isStorage(className);
			this.isWhiteListedDuringInitialization = annotations.isWhiteListedDuringInitialization(className);
		}
		catch (ClassNotFoundException e) {
			// this might be due to an incomplete jar classpath
			throw new IllegalJarException(e);
		}

		this.bootstraps = verifiedClass.getBootstraps();
		this.resolver = verifiedClass.getResolver();
		this.bcelToClass = BcelToClassTransformers.of(classLoader);
		this.cpg = verifiedClass.getConstantPool();
		this.duringInitialization = builder.duringInitialization;
	}

	protected final void issue(AbstractError error) {
		builder.issueHandler.accept(error);
		builder.setHasErrors();
	}

	protected final boolean hasWhiteListingModel(FieldInstruction fi) throws IllegalJarException {
		Optional<Field> field = resolver.resolvedFieldFor(fi);
		return field.isPresent() && classLoader.getWhiteListingWizard().whiteListingModelOf(field.get()).isPresent();
	}

	protected final boolean hasWhiteListingModel(InvokeInstruction invoke) throws IllegalJarException {
		Optional<? extends Executable> executable = resolver.resolvedExecutableFor(invoke);
		return executable.isPresent() && builder.getVerifiedClass().whiteListingModelOf(executable.get(), invoke).isPresent();
	}

	protected final LineNumberTable getLinesFor(MethodGen method) {
		return builder.getLinesFor(method);
	}

	protected final InstructionList instructionsOf(MethodGen method) {
		InstructionList instructions = method.getInstructionList();
		return instructions == null ? new InstructionList() : instructions;
	}

	/**
	 * Infers the source file name of the class being checked.
	 * If there is no debug information, the class name is returned.
	 * 
	 * @return the inferred source file name
	 */
	protected final String inferSourceFile() {
		String sourceFile = builder.getClassGen().getFileName();

		if (sourceFile != null) {
			int lastDot = className.lastIndexOf('.');
			if (lastDot > 0)
				return className.substring(0, lastDot).replace('.', '/') + '/' + sourceFile;
			else
				return sourceFile;
		}

		return className;
	}

	/**
	 * Yields the source line number from which the given instruction of the given method was compiled.
	 * 
	 * @param method the method
	 * @param pc the program point of the instruction
	 * @return the line number, or -1 if not available
	 */
	protected final int lineOf(MethodGen method, int pc) {
		LineNumberTable lines = getLinesFor(method);
		return lines != null ? lines.getSourceLine(pc) : -1;
	}

	/**
	 * Yields the source line number from which the given instruction of the given method was compiled.
	 * 
	 * @param method the method
	 * @param ih the instruction
	 * @return the line number, or -1 if not available
	 */
	protected final int lineOf(MethodGen method, InstructionHandle ih) {
		return lineOf(method, ih.getPosition());
	}

	/**
	 * Determines if this class is synthetic.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected final boolean isSynthetic() {
		return builder.getClassGen().isSynthetic();
	}

	/**
	 * Yields the lambda bridge method called by the given bootstrap.
	 * It must belong to the same class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @return the lambda bridge method
	 * @throws IllegalJarException if the jar under verification is illegal
	 */
	protected final Optional<MethodGen> getLambdaFor(BootstrapMethod bootstrap) throws IllegalJarException {
		if (bootstrap.getNumBootstrapArguments() == 3) {
			int[] bootstrapArguments = bootstrap.getBootstrapArguments();
			if (bootstrapArguments.length <= 1)
				throw new IllegalJarException("Illegal bootstrap arguments length: " + bootstrapArguments.length);

			Constant constant = cpg.getConstant(bootstrapArguments[1]);
			if (constant instanceof ConstantMethodHandle cmh) {
				Constant constant2 = cpg.getConstant(cmh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref cmr) {
					if (!(cpg.getConstant(cmr.getClassIndex()) instanceof ConstantClass cc))
						throw new IllegalJarException("Illegal constant");

					int classNameIndex = cc.getNameIndex();

					if (!(cpg.getConstant(classNameIndex) instanceof ConstantUtf8 cu8))
						throw new IllegalJarException("Illegal constant");

					String className = cu8.getBytes().replace('/', '.');

					if (!(cpg.getConstant(cmr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
						throw new IllegalJarException("Illegal constant");

					if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8_2))
						throw new IllegalJarException("Illegal constant");

					String methodName = cu8_2.getBytes();

					if (!(cpg.getConstant(nt.getSignatureIndex()) instanceof ConstantUtf8 cu8_3))
						throw new IllegalJarException("Illegal constant");

					String methodSignature = cu8_3.getBytes();

					// a lambda bridge can only be present in the same class that calls it
					if (className.equals(this.className))
						return getMethods()
								.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
								.findFirst();
				}
			}
		}

		return Optional.empty();
	}

	/**
	 * Yields the fields in this class.
	 * 
	 * @return the fields
	 */
	protected final Stream<org.apache.bcel.classfile.Field> getFields() {
		return Stream.of(builder.getClassGen().getFields());
	}

	/**
	 * Yields the methods inside the class being verified.
	 * 
	 * @return the methods
	 */
	protected final Stream<MethodGen> getMethods() {
		return builder.getMethods();
	}
}