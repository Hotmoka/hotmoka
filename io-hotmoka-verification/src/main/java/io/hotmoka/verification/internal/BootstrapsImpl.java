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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.verification.AnnotationUtilities;
import io.hotmoka.verification.BcelToClasses;
import io.hotmoka.verification.api.AnnotationUtility;
import io.hotmoka.verification.api.BcelToClass;
import io.hotmoka.verification.api.Bootstraps;
import io.hotmoka.verification.api.IllegalJarException;

/**
 * An object that provides utility methods about the lambda bootstraps
 * contained in a class.
 */
public class BootstrapsImpl implements Bootstraps {

	/**
	 * The class whose bootstraps are considered.
	 */
	private final VerifiedClassImpl verifiedClass;

	/**
	 * A utility to transform BCEL types into classes.
	 */
	private final BcelToClass bcelToClass;

	/**
	 * The utility used to deal with the annotations in the class.
	 */
	private final AnnotationUtility annotations;

	/**
	 * The constant pool of the class whose bootstraps are considered.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * The bootstrap methods of the class.
	 */
	private final BootstrapMethod[] bootstrapMethods;

	/**
	 * The bootstrap methods of the class that lead to an entry, possibly indirectly.
	 * We fix their order to avoid non-determinism.
	 */
	private final List<BootstrapMethod> bootstrapMethodsLeadingToFromContract = new ArrayList<>();

	/**
	 * The same as {@link #bootstrapMethodsLeadingToFromContract}, but as a set, for efficient containment check.
	 */
	private final Set<BootstrapMethod> bootstrapMethodsLeadingToFromContractAsSet = new HashSet<>();

	/**
	 * The set of lambdas that are reachable from the from contract code of the class. They can
	 * be considered as part of the code of the from contract. The order in this set is not fixed
	 * but this is OK since this set is only used for containment checks.
	 */
	private final Set<MethodGen> lambdasPartOfFromContract = new HashSet<>();

	private final static BootstrapMethod[] NO_BOOTSTRAPS = new BootstrapMethod[0];

	public BootstrapsImpl(VerifiedClassImpl clazz, MethodGen[] methods) throws IllegalJarException {
		this.verifiedClass = clazz;
		this.bcelToClass = BcelToClasses.of(clazz.getJar().getClassLoader());
		this.annotations = AnnotationUtilities.of(clazz.getJar());
		this.cpg = clazz.getConstantPool();
		this.bootstrapMethods = computeBootstraps();
		collectBootstrapsLeadingToFromContract(methods);
		collectLambdasOfFromContract(methods);
	}

	/**
	 * Creates a deep clone of the given bootstrap methods.
	 * 
	 * @param parent the object to clone
	 */
	private BootstrapsImpl(BootstrapsImpl parent) {
		this.verifiedClass = parent.verifiedClass;
		this.bcelToClass = parent.bcelToClass;
		this.annotations = parent.annotations;
		this.cpg = parent.cpg;
		this.bootstrapMethods = new BootstrapMethod[parent.bootstrapMethods.length];
		for (int pos = 0; pos < parent.bootstrapMethods.length; pos++) {
			BootstrapMethod clone = this.bootstrapMethods[pos] = parent.bootstrapMethods[pos].copy();
			// the array of arguments is shared by copy(), hence we clone it explicitly
			clone.setBootstrapArguments(parent.bootstrapMethods[pos].getBootstrapArguments().clone());
			if (parent.bootstrapMethodsLeadingToFromContractAsSet.contains(parent.bootstrapMethods[pos])) {
				this.bootstrapMethodsLeadingToFromContractAsSet.add(clone);
				this.bootstrapMethodsLeadingToFromContract.add(clone);
			}
		}
	}

	@Override
	public Bootstraps copy() {
		return new BootstrapsImpl(this);
	}

	@Override
	public boolean lambdaIsFromContract(BootstrapMethod bootstrap) throws IllegalJarException {
		if (bootstrap.getNumBootstrapArguments() == 3 && cpg.getConstant(bootstrap.getBootstrapArguments()[1]) instanceof ConstantMethodHandle cmh
				&& cpg.getConstant(cmh.getReferenceIndex()) instanceof ConstantMethodref cmr) {

			if (!(cpg.getConstant(cmr.getClassIndex()) instanceof ConstantClass cc))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(cc.getNameIndex()) instanceof ConstantUtf8 cu8))
				throw new IllegalJarException("Illegal constant");

			String className = cu8.getBytes().replace('/', '.');

			if (!(cpg.getConstant(cmr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8_2))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(nt.getSignatureIndex()) instanceof ConstantUtf8 cu8_3))
				throw new IllegalJarException("Illegal constant");

			String methodName = cu8_2.getBytes();
			String methodSignature = cu8_3.getBytes();

			try {
				return annotations.isFromContract(className, methodName, Type.getArgumentTypes(methodSignature), Type.getReturnType(methodSignature));
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}

		return false;
	}

	@Override
	public Stream<BootstrapMethod> getBootstraps() {
		return Stream.of(bootstrapMethods);
	}

	@Override
	public Stream<BootstrapMethod> getBootstrapsLeadingToFromContract() {
		return bootstrapMethodsLeadingToFromContract.stream();
	}

	@Override
	public BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic) throws IllegalJarException {
		if (!(cpg.getConstant(invokedynamic.getIndex()) instanceof ConstantInvokeDynamic cid))
			throw new IllegalJarException("Illegal constant");

		int index = cid.getBootstrapMethodAttrIndex();
		if (index < 0 || index >= bootstrapMethods.length)
			throw new IllegalJarException("Illegal bootstrap method index: " + index);

		return bootstrapMethods[index];
	}

	@Override
	public Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap) throws IllegalJarException {
		if (cpg.getConstant(bootstrap.getBootstrapMethodRef()) instanceof ConstantMethodHandle mh
				&& cpg.getConstant(mh.getReferenceIndex()) instanceof ConstantMethodref mr) {

			if (!(cpg.getConstant(mr.getClassIndex()) instanceof ConstantClass cc))
				throw new IllegalJarException("Illegal constant");

			int classNameIndex = cc.getNameIndex();
			if (!(cpg.getConstant(classNameIndex) instanceof ConstantUtf8 cu8))
				throw new IllegalJarException("Illegal constant");

			String className = cu8.getBytes().replace('/', '.');

			if (!(cpg.getConstant(mr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8_2))
				throw new IllegalJarException("Illegal constant");

			String methodName = cu8_2.getBytes();

			if (!(cpg.getConstant(nt.getSignatureIndex()) instanceof ConstantUtf8 cu8_3))
				throw new IllegalJarException("Illegal constant");

			String methodSignature = cu8_3.getBytes();

			return getTargetOfCallSite(bootstrap, className, methodName, methodSignature);
		}

		return Optional.empty();
	}

	@Override
	public boolean isPartOfFromContract(MethodGen lambda) {
		return lambdasPartOfFromContract.contains(lambda);
	}

	/**
	 * Yields the lambda bridge method called by the given bootstrap.
	 * It must belong to the same class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @param methods the methods of the class under verification
	 * @return the lambda bridge method, if any
	 * @throws IllegalJarException if the jar of the bootstrap is illegal
	 */
	private Optional<MethodGen> getLambdaFor(BootstrapMethod bootstrap, MethodGen[] methods) throws IllegalJarException {
		if (bootstrap.getNumBootstrapArguments() == 3 && cpg.getConstant(bootstrap.getBootstrapArguments()[1]) instanceof ConstantMethodHandle mh
				&& cpg.getConstant(mh.getReferenceIndex()) instanceof ConstantMethodref mr) {

			if (!(cpg.getConstant(mr.getClassIndex()) instanceof ConstantClass cc))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(cc.getNameIndex()) instanceof ConstantUtf8 cu8))
				throw new IllegalJarException("Illegal constant");

			String className = cu8.getBytes().replace('/', '.');

			if (!(cpg.getConstant(mr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8_2))
				throw new IllegalJarException("Illegal constant");

			String methodName = cu8_2.getBytes();

			if (!(cpg.getConstant(nt.getSignatureIndex()) instanceof ConstantUtf8 cu8_3))
				throw new IllegalJarException("Illegal constant");

			String methodSignature = cu8_3.getBytes();

			// a lambda bridge can only be present in the same class that calls it
			if (className.equals(verifiedClass.getClassName()))
				return Stream.of(methods)
						.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
						.findFirst();
		}

		return Optional.empty();
	}

	private Optional<? extends Executable> getTargetOfCallSite(BootstrapMethod bootstrap, String className, String methodName, String methodSignature) throws IllegalJarException {
		if ("java.lang.invoke.LambdaMetafactory".equals(className) &&
				"metafactory".equals(methodName) &&
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {

			// this is the standard factory used to create call sites
			int[] bootstrapArgs = bootstrap.getBootstrapArguments();
			if (bootstrapArgs.length <= 1)
				throw new IllegalJarException("Illegal bootstrap arguments count: " + bootstrapArgs.length);

			if (cpg.getConstant(bootstrapArgs[1]) instanceof ConstantMethodHandle mh) {
				Constant constant2 = cpg.getConstant(mh.getReferenceIndex());

				if (constant2 instanceof ConstantMethodref mr) {
					if (!(cpg.getConstant(mr.getClassIndex()) instanceof ConstantClass cc))
						throw new IllegalJarException("Illegal constant");

					int classNameIndex = cc.getNameIndex();

					if (!(cpg.getConstant(classNameIndex) instanceof ConstantUtf8 cu8))
						throw new IllegalJarException("Illegal constant");

					String className2 = cu8.getBytes().replace('/', '.');

					if (!(cpg.getConstant(mr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
						throw new IllegalJarException("Illegal constant");

					if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8_2))
						throw new IllegalJarException("Illegal constant");

					String methodName2 = cu8_2.getBytes();

					if (!(cpg.getConstant(nt.getSignatureIndex()) instanceof ConstantUtf8 cu8_3))
						throw new IllegalJarException("Illegal constant");

					String methodSignature2 = cu8_3.getBytes();

					try {
						Class<?>[] args = bcelToClass.of(Type.getArgumentTypes(methodSignature2));
						Class<?> returnType = bcelToClass.of(Type.getReturnType(methodSignature2));

						if (Const.CONSTRUCTOR_NAME.equals(methodName2))
							return verifiedClass.getResolver().resolveConstructorWithPossiblyExpandedArgs(className2, args);
						else
							return verifiedClass.getResolver().resolveMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
					}
					catch (ClassNotFoundException e) {
						throw new IllegalJarException(e);
					}
				}
				else if (constant2 instanceof ConstantInterfaceMethodref mr) {
					if (!(cpg.getConstant(mr.getClassIndex()) instanceof ConstantClass cc))
						throw new IllegalJarException("Illegal constant");

					int classNameIndex = cc.getNameIndex();

					if (!(cpg.getConstant(classNameIndex) instanceof ConstantUtf8 cu8))
						throw new IllegalJarException("Illegal constant");

					String className2 = cu8.getBytes().replace('/', '.');

					if (!(cpg.getConstant(mr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
						throw new IllegalJarException("Illegal constant");

					if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8_2))
						throw new IllegalJarException("Illegal constant");

					String methodName2 = cu8_2.getBytes();

					if (!(cpg.getConstant(nt.getSignatureIndex()) instanceof ConstantUtf8 cu8_3))
						throw new IllegalJarException("Illegal constant");

					String methodSignature2 = cu8_3.getBytes();

					try {
						Class<?>[] args = bcelToClass.of(Type.getArgumentTypes(methodSignature2));
						Class<?> returnType = bcelToClass.of(Type.getReturnType(methodSignature2));

						return verifiedClass.getResolver().resolveInterfaceMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
					}
					catch (ClassNotFoundException e) {
						throw new IllegalJarException(e);
					}
				}
			}
		}
		else if ("java.lang.invoke.StringConcatFactory".equals(className) &&
				"makeConcatWithConstants".equals(methodName) &&
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {

			// this factory is used to create call sites that lead to string concatenation of every
			// possible argument type. We yield the Objects.toString(Object) method
			try {
				return Optional.of(Objects.class.getMethod("toString", Object.class));
			}
			catch (NoSuchMethodException | SecurityException e) { // impossible: Object.toString() exists
				throw new RuntimeException(e);
			}
		}

		return Optional.empty();
	}

	private BootstrapMethod[] computeBootstraps() {
		for (var attribute: verifiedClass.getAttributes())
			if (attribute instanceof BootstrapMethods bootstrapMethods)
				return bootstrapMethods.getBootstrapMethods();

		return NO_BOOTSTRAPS;
	}

	private void collectBootstrapsLeadingToFromContract(MethodGen[] methods) throws IllegalJarException {
		int initialSize;

		do {
			initialSize = bootstrapMethodsLeadingToFromContract.size();
			for (var bootstrap: bootstrapMethods)
				if (lambdaIsFromContract(bootstrap) || lambdaCallsFromContract(bootstrap, methods))
					addToBootstrapMethodsLeadingToFromContract(bootstrap);
		}
		while (bootstrapMethodsLeadingToFromContract.size() > initialSize);
	}

	private boolean addToBootstrapMethodsLeadingToFromContract(BootstrapMethod bm) {
		// it gets added to the list (right) only if not already seen in the set (left)
		return bootstrapMethodsLeadingToFromContractAsSet.add(bm) && bootstrapMethodsLeadingToFromContract.add(bm);
	}

	/**
	 * Collects the lambdas that are called from a {@code @@FromContract} method.
	 * 
	 * @param methods the methods of the class under verification
	 * @throws IllegalJarException if some class of the Takamaka program cannot be found
	 */
	private void collectLambdasOfFromContract(MethodGen[] methods) throws IllegalJarException {
		// the number of iterations in bounded by methods.length

		// we collect all lambdas reachable from the @FromContract methods, possibly indirectly
		// (that is, a lambda can call another lambda); we use a working set that starts with the @FromContract methods
		var ws = new LinkedList<MethodGen>();
		var className = verifiedClass.getClassName();

		for (var method: methods) {
			try {
				if (annotations.isFromContract(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
					ws.add(method);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}

		while (!ws.isEmpty()) {
			MethodGen current = ws.removeFirst();
			InstructionList instructionsList = current.getInstructionList();

			if (instructionsList != null)
				for (Instruction ins: instructionsList.getInstructions())
					if (ins instanceof INVOKEDYNAMIC invokedynamic)
						getLambdaFor(getBootstrapFor(invokedynamic), methods).ifPresent(lambda -> {
							if (lambda.isPrivate() && lambda.isSynthetic() && lambdasPartOfFromContract.add(lambda))
								ws.addLast(lambda);
						});
		}
	}

	/**
	 * Determines if the given lambda method calls a {@code @@FromContract}, possibly indirectly.
	 * 
	 * @param bootstrap the lambda method
	 * @param methods the methods of the class under verification
	 * @return true if that condition holds
	 * @throws IllegalJarException if some class of the Takamaka program cannot be loaded
	 */
	private boolean lambdaCallsFromContract(BootstrapMethod bootstrap, MethodGen[] methods) throws IllegalJarException {
		Optional<MethodGen> lambda = getLambdaFor(bootstrap, methods);
		if (lambda.isPresent()) {
			InstructionList instructions = lambda.get().getInstructionList();
			if (instructions != null)
				for (InstructionHandle ih: instructions)
					if (leadsToFromContract(ih))
						return true;
		}

		return false;
	}

	/**
	 * Determines if the given instruction calls a {@code @@FromContract}, possibly indirectly.
	 * 
	 * @param ih the instruction
	 * @return true if that condition holds
	 * @throws IllegalJarException if some class of the Takamaka program cannot be found
	 */
	private boolean leadsToFromContract(InstructionHandle ih) throws IllegalJarException {
		Instruction instruction = ih.getInstruction();
	
		if (instruction instanceof INVOKEDYNAMIC invokedynamic)
			return bootstrapMethodsLeadingToFromContractAsSet.contains(getBootstrapFor(invokedynamic));
		else if (instruction instanceof InvokeInstruction invoke && !(invoke instanceof INVOKESTATIC)) {
			if (invoke.getReferenceType(cpg) instanceof ObjectType ot) {
				try {
					return annotations.isFromContract(ot.getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
				}
				catch (ClassNotFoundException e) {
					throw new IllegalJarException(e);
				}
			}
		}

		return false;
	}
}