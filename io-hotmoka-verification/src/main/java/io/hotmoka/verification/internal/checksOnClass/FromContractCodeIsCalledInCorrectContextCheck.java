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

package io.hotmoka.verification.internal.checksOnClass;

import static io.hotmoka.exceptions.CheckRunnable.check;
import static io.hotmoka.exceptions.UncheckConsumer.uncheck;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.exceptions.UncheckPredicate;
import io.hotmoka.verification.Pushers;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.IllegalCallToFromContractError;
import io.hotmoka.verification.errors.IllegalCallToFromContractOnThisError;
import io.hotmoka.verification.errors.IllegalCallToPayableConstructorOnThis;
import io.hotmoka.verification.internal.CheckOnClasses;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that {@link io.takamaka.code.lang.FromContract} methods or constructors are called only from instance methods of contracts.
 * Moreover, it checks that, if they are called on "this", then that call occurs in an
 * {@code @FromContract} method or constructor itself.
 */
public class FromContractCodeIsCalledInCorrectContextCheck extends CheckOnClasses {

	public FromContractCodeIsCalledInCorrectContextCheck(VerifiedClassImpl.Verification builder) throws IllegalJarException {
		super(builder);

		// the set of lambda that are unreachable from static methods that are not lambdas themselves: they can call from contract code
		Set<MethodGen> lambdasUnreachableFromStaticMethods = new HashSet<>();

		boolean isContract, isStorage;

		try {
			isContract = classLoader.isContract(className);
			isStorage = classLoader.isStorage(className);
		}
		catch (ClassNotFoundException e) {
			// the class was in the class path, so this is impossible
			throw new RuntimeException(e);
		}

		if (isStorage)
			computeLambdasUnreachableFromStaticMethods(lambdasUnreachableFromStaticMethods);

		check(IllegalJarException.class, () -> {
			// 1) from contract code cannot be called from a static context
			getMethods()
				// we do not consider as static those lambdas that are apparently static, just because the compiler
				// has optimized them into a static lambda, but are actually always called from non-static calling points
				.filter(method -> (method.isStatic() && !lambdasUnreachableFromStaticMethods.contains(method)))
				.forEachOrdered(method ->
					instructionsOf(method)
						.filter(uncheck(IllegalJarException.class, this::callsFromContract))
						.map(ih -> new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
						.forEachOrdered(this::issue)
				);
	
			// from contract code called not on this can only be called from a contract
			getMethods()
				.filter(method -> !isContract)
				.forEachOrdered(method ->
					instructionsOf(method)
						.filter(uncheck(IllegalJarException.class, ih -> callsFromContract(ih) && (method.isStatic() || !callsFromContractOnThis(ih, method, method.getInstructionList()))))
						.map(ih -> new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
						.forEachOrdered(this::issue)
				);
	
			// from contract code called on this can only be called from a storage class
			getMethods()
				.filter(method -> !isStorage)
				.filter(method -> !method.isStatic())
				.forEachOrdered(method ->
					instructionsOf(method)
						.filter(UncheckPredicate.uncheck(IllegalJarException.class, ih -> callsFromContractOnThis(ih, method, method.getInstructionList())))
						.map(ih -> new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
						.forEachOrdered(this::issue)
				);
	
			// from contract code called on this can only be called by @FromContract code 
			getMethods()
				.filter(method -> !method.isStatic())
				.forEachOrdered(uncheck(IllegalJarException.class, method -> {
					boolean isInsideFromContract = bootstraps.isPartOfFromContract(method) || isFromContract(method);
					instructionsOf(method)
						.filter(uncheck(IllegalJarException.class, ih -> !isInsideFromContract && callsFromContractOnThis(ih, method, method.getInstructionList())))
						.map(ih -> new IllegalCallToFromContractOnThisError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
						.forEachOrdered(this::issue);
				}));
	
			// from contract payable constructors called on this can only be called from payable constructors
			getMethods()
				.filter(method -> !method.isStatic())
				.filter(method -> method.getName().equals(Const.CONSTRUCTOR_NAME))
				.filter(uncheck(IllegalJarException.class, method -> !isPayable(method)))
				.forEachOrdered(method ->
					instructionsOf(method)
						.filter(UncheckPredicate.uncheck(IllegalJarException.class, ih -> callsPayableFromContractConstructorOnThis(ih, method, method.getInstructionList())))
						.map(ih -> new IllegalCallToPayableConstructorOnThis(inferSourceFile(), method.getName(), lineOf(method, ih)))
						.forEachOrdered(this::issue)
					);
		});
	}

	private boolean isFromContract(MethodGen method) throws IllegalJarException {
		try {
			return annotations.isFromContract(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}
	}

	private boolean isPayable(MethodGen method) throws IllegalJarException {
		try {
			return annotations.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}
	}

	private void computeLambdasUnreachableFromStaticMethods(Set<MethodGen> lambdasUnreachableFromStaticMethods) {
		Set<MethodGen> lambdasReachableFromStaticMethods = new HashSet<>();

		// we initially compute the set of all lambdas
		Set<MethodGen> lambdas = bootstraps.getBootstraps()
			.map(this::getLambdaFor)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toSet());

		// then we consider all lambdas that might be called, directly, from a static method
		// that is not a lambda: they must be considered as reachable from a static method
		getMethods()
			.filter(MethodGen::isStatic)
			.filter(method -> !lambdas.contains(method))
			.forEach(method -> addLambdasReachableFromStatic(method, lambdasReachableFromStaticMethods));

		// then we iterate on the same lambdas that have been found to be reachable from
		// the static methods and process them, recursively
		int initialSize;
		do {
			initialSize = lambdasReachableFromStaticMethods.size();
			new HashSet<>(lambdasReachableFromStaticMethods)
				.forEach(method -> addLambdasReachableFromStatic(method, lambdasReachableFromStaticMethods));
		}
		while (lambdasReachableFromStaticMethods.size() > initialSize);

		lambdasUnreachableFromStaticMethods.addAll(lambdas);
		lambdasUnreachableFromStaticMethods.removeAll(lambdasReachableFromStaticMethods);
	}

	private void addLambdasReachableFromStatic(MethodGen method, Set<MethodGen> lambdasReachableFromStaticMethods) {
		InstructionList instructions = method.getInstructionList();
		if (instructions != null)
			StreamSupport.stream(instructions.spliterator(), false)
				.map(InstructionHandle::getInstruction)
				.filter(instruction -> instruction instanceof INVOKEDYNAMIC)
				.map(instruction -> (INVOKEDYNAMIC) instruction)
				.map(bootstraps::getBootstrapFor)
				.map(this::getLambdaFor)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(lambdasReachableFromStaticMethods::add);
	}

	/**
	 * Determines if the given instruction calls {@code @@FromContract} code.
	 * 
	 * @param ih the instruction
	 * @return true if and only if that condition holds
	 * @throws IllegalJarException if the jar under verification is illegal
	 */
	private boolean callsFromContract(InstructionHandle ih) throws IllegalJarException {
		Instruction instruction = ih.getInstruction();
		
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstraps.lambdaIsFromContract(bootstraps.getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction invoke && !(invoke instanceof INVOKESTATIC)) {
			if (invoke.getReferenceType(cpg) instanceof ObjectType receiver) {
				try {
					return annotations.isFromContract(receiver.getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
				}
				catch (ClassNotFoundException e) {
					throw new IllegalJarException(e);
				}
			}
		}

		return false;
	}

	private boolean callsFromContractOnThis(InstructionHandle ih, MethodGen method, InstructionList il) throws IllegalJarException {
		Instruction instruction = ih.getInstruction();
		if (instruction instanceof InvokeInstruction invoke && !(invoke instanceof INVOKESTATIC) && !(invoke instanceof INVOKEDYNAMIC)) {
			Type[] args = invoke.getArgumentTypes(cpg);
			int slots = Stream.of(args).mapToInt(Type::getSize).sum();

			try {
				boolean callsFromContract = invoke.getReferenceType(cpg) instanceof ObjectType receiver && annotations.isFromContract
					(receiver.getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));

				return callsFromContract &&
						Pushers.of(ih, slots + 1, method)
						.map(InstructionHandle::getInstruction)
						.allMatch(ins -> ins instanceof LoadInstruction li && li.getIndex() == 0);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}

		return false;
	}

	private boolean callsPayableFromContractConstructorOnThis(InstructionHandle ih, MethodGen method, InstructionList il) throws IllegalJarException {
		if (ih.getInstruction() instanceof INVOKESPECIAL invokespecial) {
			String methodName = invokespecial.getMethodName(cpg);
			if (Const.CONSTRUCTOR_NAME.equals(methodName)) {
				Type[] argumentTypes = invokespecial.getArgumentTypes(cpg);
				if (invokespecial.getReferenceType(cpg) instanceof ObjectType receiver) {
					int slots = Stream.of(argumentTypes).mapToInt(Type::getSize).sum();
					String classNameOfReceiver = receiver.getClassName();
					Type returnType = invokespecial.getReturnType(cpg);
					boolean callsPayableFromContract;

					try {
						callsPayableFromContract = annotations.isFromContract(classNameOfReceiver, methodName, argumentTypes, returnType) &&
								annotations.isPayable(classNameOfReceiver, methodName, argumentTypes, returnType);
					}
					catch (ClassNotFoundException e) {
						throw new IllegalJarException(e);
					}

					return callsPayableFromContract &&
						Pushers.of(ih, slots + 1, method)
							.map(InstructionHandle::getInstruction)
							.allMatch(ins -> ins instanceof LoadInstruction li && li.getIndex() == 0);	
				}
			}
		}

		return false;
	}

	/**
	 * Yields the name of the from contract code that is directly called by the given instruction.
	 * 
	 * @param ih the instruction
	 * @return the name of the from contract code
	 */
	private String nameOfFromContractCalledDirectly(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();

		if (instruction instanceof INVOKEDYNAMIC invokedynamic) {
			BootstrapMethod bootstrap = bootstraps.getBootstrapFor(invokedynamic);
			Constant constant = cpg.getConstant(bootstrap.getBootstrapArguments()[1]);
			ConstantMethodHandle mh = (ConstantMethodHandle) constant;
			Constant constant2 = cpg.getConstant(mh.getReferenceIndex());
			ConstantMethodref mr = (ConstantMethodref) constant2;
			ConstantNameAndType nt = (ConstantNameAndType) cpg.getConstant(mr.getNameAndTypeIndex());
			return ((ConstantUtf8) cpg.getConstant(nt.getNameIndex())).getBytes();
		}
		else
			return ((InvokeInstruction) instruction).getMethodName(cpg);
	}
}