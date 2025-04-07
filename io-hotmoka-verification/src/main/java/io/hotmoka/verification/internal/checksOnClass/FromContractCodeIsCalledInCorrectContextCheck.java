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
import java.util.Optional;
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
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.verification.PushersIterators;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
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

	public FromContractCodeIsCalledInCorrectContextCheck(VerifiedClassImpl.Verification builder) throws IllegalJarException, UnknownTypeException {
		super(builder);

		var methods = getMethods().toArray(MethodGen[]::new);

		// the set of lambda that are unreachable from static methods that are not lambdas themselves: they can call from contract code
		Set<MethodGen> lambdasUnreachableFromStaticMethods = computeLambdasUnreachableFromStaticMethods(methods);

		// @FromContract code cannot be called from a static context:
		// we do not consider as static those lambdas that are apparently static, just because the compiler
		// has optimized them into a static lambda, but are actually always called from non-static calling points
		for (var method: methods)
			if (method.isStatic() && !lambdasUnreachableFromStaticMethods.contains(method))
				for (var ih: instructionsOf(method)) {
					var maybeInvoke = getInvokeToFromContract(ih);
					if (maybeInvoke.isPresent())
						issue(new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(maybeInvoke.get()), lineOf(method, ih)));
				}

		// @FromContract code not called on this can only be called from a contract
		if (!isContract)
			for (var method: methods)
				for (var ih: instructionsOf(method)) {
					var maybeInvoke = getInvokeToFromContract(ih);
					if (maybeInvoke.isPresent() && (method.isStatic() || getInvokeToFromContractOnThis(ih, method).isEmpty()))
						issue(new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(maybeInvoke.get()), lineOf(method, ih)));
				}

		// @FromContract code called on this can only be called from a storage object
		if (!isStorage)
			for (var method: methods)
				if (!method.isStatic())
					for (var ih: instructionsOf(method)) {
						var maybeInvoke = getInvokeToFromContractOnThis(ih, method);
						if (maybeInvoke.isPresent())
							issue(new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(maybeInvoke.get()), lineOf(method, ih)));
					}

		// @FromContract code called on this can only be called inside @FromContract code
		for (var method: methods)
			if (!method.isStatic()) {
				boolean isInsideFromContract = bootstraps.isPartOfFromContract(method) || isFromContract(method);

				if (!isInsideFromContract)
					for (var ih: instructionsOf(method)) {
						var maybeInvoke = getInvokeToFromContractOnThis(ih, method);
						if (maybeInvoke.isPresent())
							issue(new IllegalCallToFromContractOnThisError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(maybeInvoke.get()), lineOf(method, ih)));
					}
			}

		// @FromContract payable constructors called on this can only be called from payable constructors
		for (var method: methods)
			if (!method.isStatic() && Const.CONSTRUCTOR_NAME.equals(method.getName()) && !isPayable(method))
				for (var ih: instructionsOf(method))
					if (callsPayableFromContractConstructorOnThis(ih, method))
						issue(new IllegalCallToPayableConstructorOnThis(inferSourceFile(), method.getName(), lineOf(method, ih)));
	}

	private boolean isFromContract(MethodGen method) throws UnknownTypeException {
		return annotations.isFromContract(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
	}

	private boolean isPayable(MethodGen method) throws UnknownTypeException {
		return annotations.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
	}

	private Set<MethodGen> computeLambdasUnreachableFromStaticMethods(MethodGen[] methods) throws IllegalJarException {
		// we initially compute the set of all lambdas
		var result = new HashSet<MethodGen>();
		for (var bootstrap: bootstraps.getBootstraps().toArray(BootstrapMethod[]::new))
			getLambdaFor(bootstrap).ifPresent(result::add);

		// then we consider all lambdas that might be called, directly, from a static method
		// that is not a lambda: they must be considered as reachable from a static method
		var lambdasReachableFromStaticMethods = new HashSet<MethodGen>();
		for (var method: methods)
			if (method.isStatic() && !result.contains(method))
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

		result.removeAll(lambdasReachableFromStaticMethods);

		return result;
	}

	private void addLambdasReachableFromStatic(MethodGen method, Set<MethodGen> lambdasReachableFromStaticMethods) throws IllegalJarException {
		for (var ih: instructionsOf(method))
			if (ih.getInstruction() instanceof INVOKEDYNAMIC invokedynamic)
				getLambdaFor(bootstraps.getBootstrapFor(invokedynamic))
					.ifPresent(lambdasReachableFromStaticMethods::add);
	}

	/**
	 * Determines if the instruction at the given handle calls {@code @@FromContract} code
	 * and, in such a case, returns it as an invoke instruction.
	 * 
	 * @param ih the instruction handle
	 * @return the invoke instruction to a {@code @@FromContract} contained in {@code ih}, if any
	 * @throws IllegalJarException if the jar under verification is illegal
	 */
	private Optional<InvokeInstruction> getInvokeToFromContract(InstructionHandle ih) throws IllegalJarException, UnknownTypeException {
		Instruction instruction = ih.getInstruction();

		if (instruction instanceof INVOKEDYNAMIC invokedynamic) {
			if (bootstraps.lambdaIsFromContract(bootstraps.getBootstrapFor(invokedynamic)))
				return Optional.of(invokedynamic);
		}
		else if (instruction instanceof InvokeInstruction invoke && !(invoke instanceof INVOKESTATIC) && invoke.getReferenceType(cpg) instanceof ObjectType receiver)
			if (annotations.isFromContract(receiver.getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg)))
				return Optional.of(invoke);

		return Optional.empty();
	}

	private Optional<InvokeInstruction> getInvokeToFromContractOnThis(InstructionHandle ih, MethodGen method) throws IllegalJarException, UnknownTypeException {
		if (ih.getInstruction() instanceof InvokeInstruction invoke && !(invoke instanceof INVOKESTATIC) && !(invoke instanceof INVOKEDYNAMIC)) {
			Type[] args = invoke.getArgumentTypes(cpg);
			int slots = Stream.of(args).mapToInt(Type::getSize).sum();

			boolean callsFromContract = invoke.getReferenceType(cpg) instanceof ObjectType receiver && annotations.isFromContract
					(receiver.getClassName(), invoke.getMethodName(cpg), args, invoke.getReturnType(cpg));

			if (callsFromContract && pusherIsLoad0(ih, slots + 1, method))
				return Optional.of(invoke);
		}

		return Optional.empty();
	}

	private boolean pusherIsLoad0(InstructionHandle ih, int slots, MethodGen method) throws IllegalJarException {
		var it = PushersIterators.of(ih, slots, method);

		while (it.hasNext())
			if (!(it.next().getInstruction() instanceof LoadInstruction load) || load.getIndex() != 0)
				return false;

		return true;
	}

	private boolean callsPayableFromContractConstructorOnThis(InstructionHandle ih, MethodGen method) throws IllegalJarException, UnknownTypeException {
		if (ih.getInstruction() instanceof INVOKESPECIAL invokespecial) {
			String methodName = invokespecial.getMethodName(cpg);
			if (Const.CONSTRUCTOR_NAME.equals(methodName)) {
				Type[] argumentTypes = invokespecial.getArgumentTypes(cpg);
				if (invokespecial.getReferenceType(cpg) instanceof ObjectType receiver) {
					int slots = Stream.of(argumentTypes).mapToInt(Type::getSize).sum();
					String classNameOfReceiver = receiver.getClassName();
					Type returnType = invokespecial.getReturnType(cpg);
					boolean callsPayableFromContract = annotations.isFromContract(classNameOfReceiver, methodName, argumentTypes, returnType) &&
							annotations.isPayable(classNameOfReceiver, methodName, argumentTypes, returnType);

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
	private String nameOfFromContractCalledDirectly(InvokeInstruction invoke) throws IllegalJarException {
		if (invoke instanceof INVOKEDYNAMIC invokedynamic) {
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
		else
			return invoke.getMethodName(cpg);
	}
}