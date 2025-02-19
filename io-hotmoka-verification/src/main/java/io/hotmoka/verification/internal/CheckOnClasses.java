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
import java.util.stream.StreamSupport;

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
import io.hotmoka.verification.BcelToClasses;
import io.hotmoka.verification.api.AnnotationUtility;
import io.hotmoka.verification.api.BcelToClass;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * A verification check on a class.
 */
public abstract class CheckOnClasses {
	private final VerifiedClassImpl.Verification builder;
	protected final TakamakaClassLoader classLoader;
	protected final BootstrapsImpl bootstraps;
	protected final Resolver resolver;
	protected final AnnotationUtility annotations;
	protected final BcelToClass bcelToClass;
	protected final boolean duringInitialization;
	protected final String className;
	protected final ConstantPoolGen cpg;

	protected CheckOnClasses(VerifiedClassImpl.Verification builder) {
		this.builder = builder;
		VerifiedClassImpl verifiedClass = builder.getVerifiedClass();
		this.classLoader = verifiedClass.jar.classLoader;
		this.bootstraps = verifiedClass.bootstraps;
		this.resolver = verifiedClass.getResolver();
		this.annotations = AnnotationUtilities.of(verifiedClass.jar);
		this.bcelToClass = BcelToClasses.of(verifiedClass.jar);
		this.className = verifiedClass.getClassName();
		this.cpg = verifiedClass.getConstantPool();
		this.duringInitialization = builder.duringInitialization;
	}

	protected final void issue(AbstractErrorImpl issue) {
		builder.issueHandler.accept(issue);
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

	protected final Stream<InstructionHandle> instructionsOf(MethodGen method) {
		InstructionList instructions = method.getInstructionList();
		return instructions == null ? Stream.empty() : StreamSupport.stream(instructions.spliterator(), false);
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
	 * Determines if this class is an enumeration.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected final boolean isEnum() {
		return builder.getClassGen().isEnum();
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
	 */
	protected final Optional<MethodGen> getLambdaFor(BootstrapMethod bootstrap) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			if (constant instanceof ConstantMethodHandle cmh) {
				Constant constant2 = cpg.getConstant(cmh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref cmr) {
					int classNameIndex = ((ConstantClass) cpg.getConstant(cmr.getClassIndex())).getNameIndex();
					String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(cmr.getNameAndTypeIndex());
					String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();

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