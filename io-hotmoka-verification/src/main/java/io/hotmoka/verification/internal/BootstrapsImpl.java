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

import static io.hotmoka.exceptions.CheckRunnable.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

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

	BootstrapsImpl(VerifiedClassImpl clazz, MethodGen[] methods) throws IllegalJarException {
		this.verifiedClass = clazz;
		this.bcelToClass = BcelToClasses.of(clazz.getJar());
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
	BootstrapsImpl(BootstrapsImpl parent) {
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
	public boolean lambdaIsFromContract(BootstrapMethod bootstrap) throws IllegalJarException {
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

					try {
						return annotations.isFromContract(className, methodName, Type.getArgumentTypes(methodSignature), Type.getReturnType(methodSignature));
					}
					catch (ClassNotFoundException e) {
						throw new IllegalJarException(e);
					}
				}
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
	public BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic) {
		ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());
		return bootstrapMethods[cid.getBootstrapMethodAttrIndex()];
	}

	@Override
	public Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap) throws IllegalJarException {
		Constant constant = cpg.getConstant(bootstrap.getBootstrapMethodRef());
		if (constant instanceof ConstantMethodHandle mh && cpg.getConstant(mh.getReferenceIndex()) instanceof ConstantMethodref mr) {
			int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
			String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
			ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
			String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
			String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();

			try {
				return getTargetOfCallSite(bootstrap, className, methodName, methodSignature);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
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
	 * @return the lambda bridge method
	 */
	private Optional<MethodGen> getLambdaFor(BootstrapMethod bootstrap, MethodGen[] methods) {
		if (bootstrap.getNumBootstrapArguments() == 3) {
			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			if (constant instanceof ConstantMethodHandle) {
				ConstantMethodHandle mh = (ConstantMethodHandle) constant;
				Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref) {
					ConstantMethodref mr = (ConstantMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
	
					// a lambda bridge can only be present in the same class that calls it
					if (className.equals(verifiedClass.getClassName()))
						return Stream.of(methods)
							.filter(method -> method.getName().equals(methodName) && method.getSignature().equals(methodSignature))
							.findFirst();
				}
			}
		}
	
		return Optional.empty();
	}

	private Optional<? extends Executable> getTargetOfCallSite(BootstrapMethod bootstrap, String className, String methodName, String methodSignature) throws ClassNotFoundException {
		if ("java.lang.invoke.LambdaMetafactory".equals(className) &&
				"metafactory".equals(methodName) &&
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {
	
			// this is the standard factory used to create call sites
			if (cpg.getConstant(bootstrap.getBootstrapArguments()[1]) instanceof ConstantMethodHandle mh) {
				Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref mr) {
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
					Class<?>[] args = bcelToClass.of(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = bcelToClass.of(Type.getReturnType(methodSignature2));
	
					if (Const.CONSTRUCTOR_NAME.equals(methodName2))
						return verifiedClass.getResolver().resolveConstructorWithPossiblyExpandedArgs(className2, args);
					else
						return verifiedClass.getResolver().resolveMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
				}
				else if (constant2 instanceof ConstantInterfaceMethodref mr) {
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
					Class<?>[] args = bcelToClass.of(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = bcelToClass.of(Type.getReturnType(methodSignature2));

					return verifiedClass.getResolver().resolveInterfaceMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
				}
			}
		}
		else if ("java.lang.invoke.StringConcatFactory".equals(className) &&
				"makeConcatWithConstants".equals(methodName) &&
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;".equals(methodSignature)) {
	
			// this factory is used to create call sites that lead to string concatenation of every
			// possible argument type. Generically, we yield the Objects.toString(Object) method, since
			// all parameters must be checked in order for the call to be white-listed
			try {
				return Optional.of(Objects.class.getMethod("toString", Object.class));
			}
			catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
	
		return Optional.empty();
	}

	private BootstrapMethod[] computeBootstraps() {
		Optional<BootstrapMethods> bootstraps = Stream.of(verifiedClass.getAttributes())
			.filter(attribute -> attribute instanceof BootstrapMethods)
			.map(attribute -> (BootstrapMethods) attribute)
			.findFirst();

		return bootstraps.isPresent() ? bootstraps.get().getBootstrapMethods() : NO_BOOTSTRAPS;
	}

	private void collectBootstrapsLeadingToFromContract(MethodGen[] methods) throws IllegalJarException {
		int initialSize;
		do {
			initialSize = bootstrapMethodsLeadingToFromContract.size();
			check(IllegalJarException.class, () -> getBootstraps()
				.filter(uncheck(IllegalJarException.class, bootstrap -> lambdaIsFromContract(bootstrap) || lambdaCallsFromContract(bootstrap, methods)))
				.forEach(this::addToBootstrapMethodsLeadingToFromContract));
		}
		while (bootstrapMethodsLeadingToFromContract.size() > initialSize);
	}

	private boolean addToBootstrapMethodsLeadingToFromContract(BootstrapMethod bm) {
		return bootstrapMethodsLeadingToFromContractAsSet.add(bm) && bootstrapMethodsLeadingToFromContract.add(bm);
	}

	/**
	 * Collects the lambdas that are called from a {@code @@FromContract} method.
	 * 
	 * @param methods the methods of the class under verification
	 * @throws IllegalJarException if some class of the Takamaka program cannot be found
	 */
	private void collectLambdasOfFromContract(MethodGen[] methods) throws IllegalJarException {
		// we collect all lambdas reachable from the @FromContract methods, possibly indirectly
		// (that is, a lambda can call another lambda); we use a working set that starts
		// with the @FromContract methods
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
			if (instructionsList != null) {
				Stream.of(instructionsList.getInstructions())
					.filter(ins -> ins instanceof INVOKEDYNAMIC)
					.map(ins -> (INVOKEDYNAMIC) ins)
					.map(this::getBootstrapFor)
					.map(bootstrap -> getLambdaFor(bootstrap, methods))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.filter(lambda -> lambda.isPrivate() && lambda.isSynthetic())
					.filter(lambdasPartOfFromContract::add)
					.forEach(ws::addLast);
			}
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