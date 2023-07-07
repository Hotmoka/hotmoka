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
import static io.hotmoka.exceptions.CheckSupplier.check2;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck2;

import java.lang.reflect.Executable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.hotmoka.verification.api.Bootstraps;

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
	 * The constant pool of the class whose bootstraps are considered.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * The bootstrap methods of the class.
	 */
	private final BootstrapMethod[] bootstrapMethods;

	/**
	 * The bootstrap methods of the class that lead to an entry, possibly indirectly.
	 */
	private final Set<BootstrapMethod> bootstrapMethodsLeadingToEntries = new HashSet<>();

	/**
	 * The set of lambdas that are reachable from the entries of the class. They can
	 * be considered as part of the code of the entries.
	 */
	private final Set<MethodGen> lambdasPartOfEntries = new HashSet<>();

	private final static BootstrapMethod[] NO_BOOTSTRAPS = new BootstrapMethod[0];

	BootstrapsImpl(VerifiedClassImpl clazz, MethodGen[] methods) throws ClassNotFoundException {
		this.verifiedClass = clazz;
		this.cpg = clazz.getConstantPool();
		this.bootstrapMethods = computeBootstraps();
		collectBootstrapsLeadingToEntries(methods);
		collectLambdasOfEntries(methods);
	}

	/**
	 * Creates a deep clone of the given bootstrap methods.
	 * 
	 * @param original the object to clone
	 */
	BootstrapsImpl(BootstrapsImpl original) {
		this.verifiedClass = original.verifiedClass;
		this.cpg = original.cpg;
		this.bootstrapMethods = new BootstrapMethod[original.bootstrapMethods.length];
		for (int pos = 0; pos < original.bootstrapMethods.length; pos++) {
			BootstrapMethod clone = this.bootstrapMethods[pos] = original.bootstrapMethods[pos].copy();
			// the array of arguments is shared by copy(), hence we clone it explicitly
			clone.setBootstrapArguments(original.bootstrapMethods[pos].getBootstrapArguments().clone());
			if (original.bootstrapMethodsLeadingToEntries.contains(original.bootstrapMethods[pos]))
				this.bootstrapMethodsLeadingToEntries.add(clone);
		}
	}

	@Override
	public boolean lambdaIsEntry(BootstrapMethod bootstrap) throws ClassNotFoundException {
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

					return verifiedClass.jar.annotations.isFromContract(className, methodName, Type.getArgumentTypes(methodSignature), Type.getReturnType(methodSignature));
				}
			}
		}

		return false;
	}

	@Override
	public boolean lambdaIsRedPayable(BootstrapMethod bootstrap) throws ClassNotFoundException {
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

					return verifiedClass.jar.annotations.isRedPayable(className, methodName, Type.getArgumentTypes(methodSignature), Type.getReturnType(methodSignature));
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
	public Stream<BootstrapMethod> getBootstrapsLeadingToEntries() {
		return bootstrapMethodsLeadingToEntries.stream();
	}

	@Override
	public BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic) {
		ConstantInvokeDynamic cid = (ConstantInvokeDynamic) cpg.getConstant(invokedynamic.getIndex());
		return bootstrapMethods[cid.getBootstrapMethodAttrIndex()];
	}

	@Override
	public Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap) throws ClassNotFoundException {
		Constant constant = cpg.getConstant(bootstrap.getBootstrapMethodRef());
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
	
				return getTargetOfCallSite(bootstrap, className, methodName, methodSignature);
			}
		}
	
		return Optional.empty();
	}

	@Override
	public boolean isPartOfFromContract(MethodGen lambda) {
		return lambdasPartOfEntries.contains(lambda);
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
			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			if (constant instanceof ConstantMethodHandle) {
				ConstantMethodHandle mh = (ConstantMethodHandle) constant;
				Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
				if (constant2 instanceof ConstantMethodref) {
					ConstantMethodref mr = (ConstantMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
					Class<?>[] args = verifiedClass.jar.bcelToClass.of(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = verifiedClass.jar.bcelToClass.of(Type.getReturnType(methodSignature2));
	
					if (Const.CONSTRUCTOR_NAME.equals(methodName2))
						return verifiedClass.resolver.resolveConstructorWithPossiblyExpandedArgs(className2, args);
					else
						return verifiedClass.resolver.resolveMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
				}
				else if (constant2 instanceof ConstantInterfaceMethodref) {
					ConstantInterfaceMethodref mr = (ConstantInterfaceMethodref) constant2;
					int classNameIndex = ((ConstantClass) cpg.getConstant(mr.getClassIndex())).getNameIndex();
					String className2 = ((ConstantUtf8) cpg.getConstant(classNameIndex)).getBytes().replace('/', '.');
					ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
					String methodName2 = ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
					String methodSignature2 = ((ConstantUtf8) cpg.getConstant(nt.getSignatureIndex())).getBytes();
					Class<?>[] args = verifiedClass.jar.bcelToClass.of(Type.getArgumentTypes(methodSignature2));
					Class<?> returnType = verifiedClass.jar.bcelToClass.of(Type.getReturnType(methodSignature2));

					return verifiedClass.resolver.resolveInterfaceMethodWithPossiblyExpandedArgs(className2, methodName2, args, returnType);
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
				throw new RuntimeException("unexpected exception", e);
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

	private void collectBootstrapsLeadingToEntries(MethodGen[] methods) throws ClassNotFoundException {
		int initialSize;
		do {
			initialSize = bootstrapMethodsLeadingToEntries.size();
			check2(ClassNotFoundException.class, () -> getBootstraps()
				.filter(uncheck2(bootstrap -> lambdaIsEntry(bootstrap) || lambdaCallsEntry(bootstrap, methods)))
				.forEach(bootstrapMethodsLeadingToEntries::add));
		}
		while (bootstrapMethodsLeadingToEntries.size() > initialSize);
	}

	/**
	 * Collects the lambdas that are called from an {@code @@Entry} method.
	 * 
	 * @param methods the methods of the class under verification
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	private void collectLambdasOfEntries(MethodGen[] methods) throws ClassNotFoundException {
		// we collect all lambdas reachable from the @Entry methods, possibly indirectly
		// (that is, a lambda can call another lambda); we use a working set that starts
		// with the @Entry methods
		LinkedList<MethodGen> ws = new LinkedList<>();
		check2(ClassNotFoundException.class, () ->
			Stream.of(methods)
				.filter(uncheck2(method -> verifiedClass.jar.annotations.isFromContract(verifiedClass.getClassName(), method.getName(), method.getArgumentTypes(), method.getReturnType())))
				.forEach(ws::add)
		);

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
					.filter(lambdasPartOfEntries::add)
					.forEach(ws::addLast);
			}
		}
	}

	/**
	 * Determines if the given lambda method calls an {@code @@Entry}, possibly indirectly.
	 * 
	 * @param bootstrap the lambda method
	 * @param methods the methods of the class under verification
	 * @return true if that condition holds
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	private boolean lambdaCallsEntry(BootstrapMethod bootstrap, MethodGen[] methods) throws ClassNotFoundException {
		Optional<MethodGen> lambda = getLambdaFor(bootstrap, methods);
		if (lambda.isPresent()) {
			InstructionList instructions = lambda.get().getInstructionList();
			if (instructions != null)
				return check2(ClassNotFoundException.class, () ->
					StreamSupport.stream(instructions.spliterator(), false).anyMatch(uncheck2(this::leadsToEntry))
				);
		}

		return false;
	}

	/**
	 * Determines if the given instruction calls an {@code @@Entry}, possibly indirectly.
	 * 
	 * @param ih the instruction
	 * @return true if that condition holds
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	private boolean leadsToEntry(InstructionHandle ih) throws ClassNotFoundException {
		Instruction instruction = ih.getInstruction();
	
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstrapMethodsLeadingToEntries.contains(getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC)) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			return receiver instanceof ObjectType &&
				verifiedClass.jar.annotations.isFromContract
					(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
		}
		else
			return false;
	}
}