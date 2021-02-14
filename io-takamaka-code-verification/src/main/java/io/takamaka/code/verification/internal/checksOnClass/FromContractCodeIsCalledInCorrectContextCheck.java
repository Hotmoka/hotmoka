package io.takamaka.code.verification.internal.checksOnClass;

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
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.internal.CheckOnClasses;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalCallToFromContractError;
import io.takamaka.code.verification.issues.IllegalCallToFromContractOnThisError;
import io.takamaka.code.verification.issues.IllegalCallToPayableConstructorOnThis;
import io.takamaka.code.verification.issues.IllegalCallToRedPayableConstructorOnThis;

/**
 * A check that {@link io.takamaka.code.lang.FromContract} methods or constructors are called only from instance methods of contracts.
 * Moreover, it checks that, if they are called on "this", then that call occurs in an
 * {@code @FromContract} method or constructor itself.
 */
public class FromContractCodeIsCalledInCorrectContextCheck extends CheckOnClasses {

	public FromContractCodeIsCalledInCorrectContextCheck(VerifiedClassImpl.Builder builder) {
		super(builder);

		// the set of lambda that are unreachable from static methods that are not lambdas themselves: they can call from contract code
		Set<MethodGen> lambdasUnreachableFromStaticMethods = new HashSet<>();
		boolean isContract = classLoader.isContract(className);
		boolean isStorage = classLoader.isStorage(className);
		if (isStorage)
			computeLambdasUnreachableFromStaticMethods(lambdasUnreachableFromStaticMethods);

		// 1) from contract code cannot be called from a static context
		getMethods()
			// we do not consider as static those lambdas that are apparently static, just because the compiler
			// has optimized them into a static lambda, but are actually always called from non-static calling points
			.filter(method -> (method.isStatic() && !lambdasUnreachableFromStaticMethods.contains(method)))
			.forEachOrdered(method ->
				instructionsOf(method)
					.filter(this::callsFromContract)
					.map(ih -> new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
					.forEachOrdered(this::issue)
			);

		// from contract code called not on this can only be called from a contract
		getMethods()
			.filter(method -> !isContract)
			.forEachOrdered(method ->
				instructionsOf(method)
					.filter(ih -> callsFromContract(ih) && (method.isStatic() || !callsFromContractOnThis(ih, method.getInstructionList())))
					.map(ih -> new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
					.forEachOrdered(this::issue)
			);

		// from contract code called on this can only be called from a storage class
		getMethods()
			.filter(method -> !isStorage)
			.filter(method -> !method.isStatic())
			.forEachOrdered(method ->
				instructionsOf(method)
					.filter(ih -> callsFromContractOnThis(ih, method.getInstructionList()))
					.map(ih -> new IllegalCallToFromContractError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
					.forEachOrdered(this::issue)
			);

		// from contract code called on this can only be called by @FromContract code 
		getMethods()
			.filter(method -> !method.isStatic())
			.forEachOrdered(method -> {
				boolean isInsideFromContract = bootstraps.isPartOfFromContract(method) || annotations.isFromContract(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
				instructionsOf(method)
					.filter(ih -> !isInsideFromContract && callsFromContractOnThis(ih, method.getInstructionList()))
					.map(ih -> new IllegalCallToFromContractOnThisError(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
					.forEachOrdered(this::issue);
			});

		// from contract payable constructors called on this can only be called from payable constructors
		getMethods()
			.filter(method -> !method.isStatic())
			.filter(method -> method.getName().equals(Const.CONSTRUCTOR_NAME))
			.filter(method -> !annotations.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.forEachOrdered(method ->
				instructionsOf(method)
					.filter(ih -> callsPayableFromContractConstructorOnThis(ih, method.getInstructionList()))
					.map(ih -> new IllegalCallToPayableConstructorOnThis(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
					.forEachOrdered(this::issue)
				);

		// from contract red-payable constructors called on this can only be called from red-payable constructors
		getMethods()
			.filter(method -> !method.isStatic())
			.filter(method -> method.getName().equals(Const.CONSTRUCTOR_NAME))
			.filter(method -> !annotations.isRedPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.forEachOrdered(method ->
				instructionsOf(method)
					.filter(ih -> callsRedPayableFromContractConstructorOnThis(ih, method.getInstructionList()))
					.map(ih -> new IllegalCallToRedPayableConstructorOnThis(inferSourceFile(), method.getName(), nameOfFromContractCalledDirectly(ih), lineOf(method, ih)))
					.forEachOrdered(this::issue)
				);
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
			new HashSet<>(lambdasReachableFromStaticMethods).stream()
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
	 */
	private boolean callsFromContract(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();
		
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstraps.lambdaIsEntry(bootstraps.getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC)) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			return receiver instanceof ObjectType
				&& annotations.isFromContract
					(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
		}
		else
			return false;
	}

	private boolean callsFromContractOnThis(InstructionHandle ih, InstructionList il) {
		Instruction instruction = ih.getInstruction();
		if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC) && !(instruction instanceof INVOKEDYNAMIC)) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			Type[] args = invoke.getArgumentTypes(cpg);
			ReferenceType receiver = invoke.getReferenceType(cpg);
			int slots = Stream.of(args).mapToInt(Type::getSize).sum();
			boolean callsFromContract = receiver instanceof ObjectType && annotations.isFromContract
				(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));

			if (callsFromContract) {
				Runnable error = () -> {
					throw new IllegalStateException("Cannot find stack pushers");
				};

				return pushers.getPushers(ih, slots + 1, il, cpg, error)
					.map(InstructionHandle::getInstruction)
					.allMatch(ins -> ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0);	
			}
		}

		return false;
	}

	private boolean callsPayableFromContractConstructorOnThis(InstructionHandle ih, InstructionList il) {
		Instruction instruction = ih.getInstruction();
		if (instruction instanceof INVOKESPECIAL) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			String methodName = invoke.getMethodName(cpg);
			if (Const.CONSTRUCTOR_NAME.equals(methodName)) {
				Type[] argumentTypes = invoke.getArgumentTypes(cpg);
				ReferenceType receiver = invoke.getReferenceType(cpg);
				if (receiver instanceof ObjectType) {
					int slots = Stream.of(argumentTypes).mapToInt(Type::getSize).sum();
					String classNameOfReceiver = ((ObjectType) receiver).getClassName();
					Type returnType = invoke.getReturnType(cpg);
					boolean callsPayableFromContract = annotations.isFromContract(classNameOfReceiver, methodName, argumentTypes, returnType) &&
						annotations.isPayable(classNameOfReceiver, methodName, argumentTypes, returnType);

					if (callsPayableFromContract) {
						Runnable error = () -> {
							throw new IllegalStateException("Cannot find stack pushers");
						};

						return pushers.getPushers(ih, slots + 1, il, cpg, error)
							.map(InstructionHandle::getInstruction)
							.allMatch(ins -> ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0);	
					}
				}
			}
		}

		return false;
	}

	private boolean callsRedPayableFromContractConstructorOnThis(InstructionHandle ih, InstructionList il) {
		Instruction instruction = ih.getInstruction();
		if (instruction instanceof INVOKESPECIAL) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			String methodName = invoke.getMethodName(cpg);
			if (Const.CONSTRUCTOR_NAME.equals(methodName)) {
				Type[] argumentTypes = invoke.getArgumentTypes(cpg);
				Type[] args = argumentTypes;
				ReferenceType receiver = invoke.getReferenceType(cpg);
				if (receiver instanceof ObjectType) {
					int slots = Stream.of(args).mapToInt(Type::getSize).sum();
					String classNameOfReceiver = ((ObjectType) receiver).getClassName();
					Type returnType = invoke.getReturnType(cpg);
					boolean callsPayableFromContract = annotations.isFromContract(classNameOfReceiver, methodName, argumentTypes, returnType) &&
						annotations.isRedPayable(classNameOfReceiver, methodName, argumentTypes, returnType);

					if (callsPayableFromContract) {
						Runnable error = () -> {
							throw new IllegalStateException("Cannot find stack pushers");
						};

						return pushers.getPushers(ih, slots + 1, il, cpg, error)
							.map(InstructionHandle::getInstruction)
							.allMatch(ins -> ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0);	
					}
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

		if (instruction instanceof INVOKEDYNAMIC) {
			BootstrapMethod bootstrap = bootstraps.getBootstrapFor((INVOKEDYNAMIC) instruction);
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