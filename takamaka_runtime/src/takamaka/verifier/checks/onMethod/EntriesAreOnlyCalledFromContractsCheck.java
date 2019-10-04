package takamaka.verifier.checks.onMethod;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalCallToEntryError;

/**
 * A check that {@code @@Entry} methods or constructors are called only from instance methods of contracts.
 */
public class EntriesAreOnlyCalledFromContractsCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public EntriesAreOnlyCalledFromContractsCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		if (!classLoader.isContract(className) || (method.isStatic() && mightBeReachedFromStaticMethods(method)))
			instructions()
				.filter(ih -> clazz.getClassBootstraps().callsEntry(ih, false))
				.map(ih -> new IllegalCallToEntryError(clazz, method, nameOfEntryCalledDirectly(ih), lineOf(ih)))
				.forEach(this::issue);
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