package takamaka.verifier.checks.onMethod;

import org.apache.bcel.Const;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.JsrInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.StoreInstruction;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalJsrInstructionError;
import takamaka.verifier.errors.IllegalPutstaticInstructionError;
import takamaka.verifier.errors.IllegalRetInstructionError;
import takamaka.verifier.errors.IllegalSynchronizationError;
import takamaka.verifier.errors.IllegalUpdateOfLocal0Error;

/**
 * A check that the method has no unusual bytecodes, such as {@code jsr}, {@code ret}
 * or updates of local 0 in instance methods. Such bytecodes are allowed in
 * Java bytecode, although they are never generated by modern compilers. Takamaka forbids them
 * since they make code verification more difficult.
 */
public class BytecodesAreLegalCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public BytecodesAreLegalCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		instructions().forEach(this::checkIfItIsIllegal);
	}

	private void checkIfItIsIllegal(InstructionHandle ih) {
		Instruction ins = ih.getInstruction();

		if (ins instanceof PUTSTATIC) {
			// static field updates are allowed inside the synthetic methods or static initializer,
			// for instance in an enumeration
			if (!method.isSynthetic() && !method.getName().equals(Const.STATIC_INITIALIZER_NAME))
				issue(new IllegalPutstaticInstructionError(clazz, method.getName(), lineOf(ih)));
		}
		else if (ins instanceof JsrInstruction)
			issue(new IllegalJsrInstructionError(clazz, method.getName(), lineOf(ih)));
		else if (ins instanceof RET)
			issue(new IllegalRetInstructionError(clazz, method.getName(), lineOf(ih)));
		else if (!method.isStatic() && ins instanceof StoreInstruction && ((StoreInstruction) ins).getIndex() == 0)
			issue(new IllegalUpdateOfLocal0Error(clazz, method.getName(), lineOf(ih)));					
		else if (ins instanceof MONITORENTER || ins instanceof MONITOREXIT)
			issue(new IllegalSynchronizationError(clazz, method.getName(), lineOf(ih)));
	}
}