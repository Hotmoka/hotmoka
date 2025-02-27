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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
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

		if (isStorage)
			computeLambdasUnreachableFromStaticMethods(lambdasUnreachableFromStaticMethods);

		MethodGen[] methods = getMethods().toArray(MethodGen[]::new);

		// from contract code cannot be called from a static context:
		// we do not consider as static those lambdas that are apparently static, just because the compiler
		// has optimized them into a static lambda, but are actually always called from non-static calling points
		for (var method: methods)
			if (method.isStatic() && !lambdasUnreachableFromStaticMethods.contains(method))
				for (var ih: instructionsOf(method))
					if (callsFromContract(ih))
						issue(new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)));

		// from contract code called not on this can only be called from a contract
		for (var method: methods)
			if (!isContract)
				for (var ih: instructionsOf(method))
					if (callsFromContract(ih) && (method.isStatic() || !callsFromContractOnThis(ih, method, method.getInstructionList())))
						issue(new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)));

		// from contract code called not on this can only be called from a contract
		for (var method: methods)
			if (!isContract)
				for (var ih: instructionsOf(method))
					if (callsFromContract(ih) && (method.isStatic() || !callsFromContractOnThis(ih, method, method.getInstructionList())))
						issue(new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)));

		for (var method: methods)
			if (!isStorage && !method.isStatic())
				for (var ih: instructionsOf(method))
					if (callsFromContractOnThis(ih, method, method.getInstructionList()))
						issue(new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)));

		// from contract code called on this can only be called by @FromContract code
		for (var method: methods)
			if (!method.isStatic()) {
				boolean isInsideFromContract = bootstraps.isPartOfFromContract(method) || isFromContract(method);

				for (var ih: instructionsOf(method))
					if (!isInsideFromContract && callsFromContractOnThis(ih, method, method.getInstructionList()))
						issue(new IllegalCallToFromContractOnThisError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)));
			}

		// from contract payable constructors called on this can only be called from payable constructors
		for (var method: methods)
			if (!method.isStatic() && Const.CONSTRUCTOR_NAME.equals(method.getName()) && !isPayable(method))
				for (var ih: instructionsOf(method))
					if (callsPayableFromContractConstructorOnThis(ih, method, method.getInstructionList()))
						issue(new IllegalCallToPayableConstructorOnThis(inferSourceFile(), method.getName(), lineOf(method, ih)));
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

	private void computeLambdasUnreachableFromStaticMethods(Set<MethodGen> lambdasUnreachableFromStaticMethods) throws IllegalJarException {
		Set<MethodGen> lambdasReachableFromStaticMethods = new HashSet<>();

		// we initially compute the set of all lambdas
		Set<MethodGen> lambdas = new HashSet<>();
		
		for (var bootstrap: bootstraps.getBootstraps().toArray(BootstrapMethod[]::new))
			getLambdaFor(bootstrap).ifPresent(lambdas::add);

		// then we consider all lambdas that might be called, directly, from a static method
		// that is not a lambda: they must be considered as reachable from a static method
		for (var method: getMethods().toArray(MethodGen[]::new))
			if (method.isStatic() && !lambdas.contains(method))
				addLambdasReachableFromStatic(method, lambdasReachableFromStaticMethods);

		// then we iterate on the same lambdas that have been found to be reachable from
		// the static methods and process them, recursively
		int initialSize;
		do {
			initialSize = lambdasReachableFromStaticMethods.size();
			for (var method: new HashSet<>(lambdasReachableFromStaticMethods))
				addLambdasReachableFromStatic(method, lambdasReachableFromStaticMethods);
		}
		while (lambdasReachableFromStaticMethods.size() > initialSize);

		lambdasUnreachableFromStaticMethods.addAll(lambdas);
		lambdasUnreachableFromStaticMethods.removeAll(lambdasReachableFromStaticMethods);
	}

	private void addLambdasReachableFromStatic(MethodGen method, Set<MethodGen> lambdasReachableFromStaticMethods) throws IllegalJarException {
		for (var ih: instructionsOf(method))
			if (ih.getInstruction() instanceof INVOKEDYNAMIC invokedynamic)
				getLambdaFor(bootstraps.getBootstrapFor(invokedynamic))
					.ifPresent(lambdasReachableFromStaticMethods::add);
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

		if (instruction instanceof INVOKEDYNAMIC invokedynamic)
			return bootstraps.lambdaIsFromContract(bootstraps.getBootstrapFor(invokedynamic));
		else if (instruction instanceof InvokeInstruction invoke && !(invoke instanceof INVOKESTATIC) && invoke.getReferenceType(cpg) instanceof ObjectType receiver) {
			try {
				return annotations.isFromContract(receiver.getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}

		return false;
	}

	private boolean callsFromContractOnThis(InstructionHandle ih, MethodGen method, InstructionList il) throws IllegalJarException {
		if (ih.getInstruction() instanceof InvokeInstruction invoke && !(invoke instanceof INVOKESTATIC) && !(invoke instanceof INVOKEDYNAMIC)) {
			Type[] args = invoke.getArgumentTypes(cpg);
			int slots = Stream.of(args).mapToInt(Type::getSize).sum();

			try {
				boolean callsFromContract = invoke.getReferenceType(cpg) instanceof ObjectType receiver && annotations.isFromContract
					(receiver.getClassName(), invoke.getMethodName(cpg), args, invoke.getReturnType(cpg));

				return callsFromContract && pusherIsLoad0(ih, slots + 1, method);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}

		return false;
	}

	private boolean pusherIsLoad0(InstructionHandle ih, int slots, MethodGen method) throws IllegalJarException {
		var it = Pushers.iterator(ih, slots, method);

		while (it.hasNext())
			if (!(it.next().getInstruction() instanceof LoadInstruction load) || load.getIndex() != 0)
				return false;

		return true;
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

					return callsPayableFromContract && pusherIsLoad0(ih, slots + 1, method);
				}
			}
		}

		return false;
	}

	/**
	 * Yields the name of the from contract code that is directly called by the given invoke instruction.
	 * 
	 * @param ih the invoke instruction
	 * @return the name of the from contract code
	 * @throws IllegalJarException if the jar under analysis is illegal
	 */
	private String nameOfFromContractCalledDirectly(InstructionHandle ih) throws IllegalJarException {
		Instruction instruction = ih.getInstruction();

		if (instruction instanceof INVOKEDYNAMIC invokedynamic) {
			BootstrapMethod bootstrap = bootstraps.getBootstrapFor(invokedynamic);

			int[] bootstrapArgs = bootstrap.getBootstrapArguments();
			if (bootstrapArgs.length <= 1 || !(cpg.getConstant(bootstrapArgs[1]) instanceof ConstantMethodHandle mh))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(mh.getReferenceIndex()) instanceof ConstantMethodref mr))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(mr.getNameAndTypeIndex()) instanceof ConstantNameAndType nt))
				throw new IllegalJarException("Illegal constant");

			if (!(cpg.getConstant(nt.getNameIndex()) instanceof ConstantUtf8 cu8))
				throw new IllegalJarException("Illegal constant");

			return cu8.getBytes();
		}
		else // this method is called only on invoke instructions // TODO: avoid cast in the future
			return ((InvokeInstruction) instruction).getMethodName(cpg);
	}
}