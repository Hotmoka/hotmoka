package takamaka.verifier.checks.onMethod;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalCallToEntryError;

/**
 * A check that {@code @@Entry} methods or constructors are called only from instance methods of contracts.
 */
public class EntriesAreOnlyCalledFromContractsCheck extends VerifiedClassGen.Verification.Check {

	/**
	 * The set of lambda that are unreachable from static methods that are not lambdas themselves:
	 * they can call entries.
	 */
	private final Set<Method> lambdasUnreachableFromStaticMethods = new HashSet<>();

	public EntriesAreOnlyCalledFromContractsCheck(VerifiedClassGen.Verification verification) {
		verification.super();

		boolean isContract = classLoader.isContract(className);
		if (isContract)
			computeLambdasUnreachableFromStaticMethods();

		for (Method method: clazz.getMethods())
			if (!isContract || (method.isStatic() && !lambdasUnreachableFromStaticMethods.contains(method)))
				instructionsOf(method)
					.filter(this::callsEntry)
					.map(ih -> new IllegalCallToEntryError(clazz, method.getName(), nameOfEntryCalledDirectly(ih), lineOf(method, ih)))
					.forEach(this::issue);
	}

	private void computeLambdasUnreachableFromStaticMethods() {
		Set<Method> lambdasReachableFromStaticMethods = new HashSet<>();

		// we initially compute the set of all lambdas
		Set<Method> lambdas = clazz.getClassBootstraps().getBootstraps()
			.map(clazz.getClassBootstraps()::getLambdaFor)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toSet());

		// then we consider all lambdas that might be called, directly, from a static method
		// that is not a lambda: they must be considered as reachable from a static method
		Stream.of(clazz.getMethods())
			.filter(Method::isStatic)
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

	private void addLambdasReachableFromStatic(Method method, Set<Method> lambdasReachableFromStaticMethods) {
		InstructionList instructions = getMethodGenFor(method).getInstructionList();
		if (instructions != null)
			StreamSupport.stream(instructions.spliterator(), false)
				.map(InstructionHandle::getInstruction)
				.filter(instruction -> instruction instanceof INVOKEDYNAMIC)
				.map(instruction -> (INVOKEDYNAMIC) instruction)
				.map(clazz.getClassBootstraps()::getBootstrapFor)
				.map(clazz.getClassBootstraps()::getLambdaFor)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.forEach(lambdasReachableFromStaticMethods::add);
	}

	/**
	 * Determines if the given instruction calls an {@code @@Entry}.
	 * 
	 * @param ih the instruction
	 * @return true if and only if that condition holds
	 */
	private boolean callsEntry(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();
		
		if (instruction instanceof INVOKEDYNAMIC)
			return clazz.getClassBootstraps().lambdaIsEntry(clazz.getClassBootstraps().getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC)) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			return receiver instanceof ObjectType &&
				classLoader.isEntryPossiblyAlreadyInstrumented
					(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getSignature(cpg));
		}
		else
			return false;
	}

	/**
	 * Yields the name of the entry that is directly called by the given instruction.
	 * 
	 * @param ih the instruction
	 * @return the name of the entry
	 */
	private String nameOfEntryCalledDirectly(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();

		if (instruction instanceof INVOKEDYNAMIC) {
			BootstrapMethod bootstrap = clazz.getClassBootstraps().getBootstrapFor((INVOKEDYNAMIC) instruction);
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