package io.takamaka.code.verification.internal.checksOnClass;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalCallToRedPayableError;

/**
 * A check that {@code @@RedPayable} methods or constructors are called only from red/green contracts.
 */
public class RedPayableIsOnlyCalledFromRedGreenContractsCheck extends VerifiedClassImpl.Builder.Check {

	public RedPayableIsOnlyCalledFromRedGreenContractsCheck(VerifiedClassImpl.Builder verification) {
		verification.super();

		if (!classLoader.isRedGreenContract(className))
			getMethods()
				.forEachOrdered(method ->
					instructionsOf(method)
						.filter(this::callsRedPayable)
						.map(ih -> new IllegalCallToRedPayableError(inferSourceFile(), method.getName(), nameOfEntryCalledDirectly(ih), lineOf(method, ih)))
						.forEachOrdered(this::issue)
				);
	}

	/**
	 * Determines if the given instruction calls a {@code @@RedPayable} method or constructor.
	 * 
	 * @param ih the instruction
	 * @return true if and only if that condition holds
	 */
	private boolean callsRedPayable(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();
		
		if (instruction instanceof INVOKEDYNAMIC)
			return bootstraps.lambdaIsRedPayable(bootstraps.getBootstrapFor((INVOKEDYNAMIC) instruction));
		else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC)) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			ReferenceType receiver = invoke.getReferenceType(cpg);
			return receiver instanceof ObjectType
				&& annotations.isRedPayable
					(((ObjectType) receiver).getClassName(), invoke.getMethodName(cpg), invoke.getArgumentTypes(cpg), invoke.getReturnType(cpg));
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