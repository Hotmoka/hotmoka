package takamaka.verifier.checks.onMethod;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.CallerNotOnThisError;
import takamaka.verifier.errors.CallerOutsideEntryError;

/**
 * A check that {@code caller()} is only used with {@code this} as receiver
 * and inside an {@code @@Entry} method or constructor.
 */
public class CallerIsUsedOnThisAndInEntryCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public CallerIsUsedOnThisAndInEntryCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		boolean isEntry = classLoader.isEntry(className, methodName, methodArgs, methodReturnType).isPresent();

		instructions()
			.filter(this::isCallToContractCaller)
			.forEach(ih -> {
				if (!isEntry)
					issue(new CallerOutsideEntryError(clazz, methodName, lineOf(ih)));

				if (!previousIsLoad0(ih))
					issue(new CallerNotOnThisError(clazz, methodName, lineOf(ih)));
			});
	}

	private boolean previousIsLoad0(InstructionHandle ih) {
		// we skip NOPs
		for (ih = ih.getPrev(); ih != null && ih.getInstruction() instanceof NOP; ih = ih.getPrev());

		Instruction ins;
		return ih != null && (ins = ih.getInstruction()) instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0;
	}

	/**
	 * The Java bytecode types of the {@code caller()} method of {@link #takamaka.lang.Contract}.
	 */
	private final static String TAKAMAKA_CALLER_SIG = "()Ltakamaka/lang/Contract;";

	private boolean isCallToContractCaller(InstructionHandle ih) {
		Instruction ins = ih.getInstruction();
		if (ins instanceof InvokeInstruction) {
			InvokeInstruction invoke = (InvokeInstruction) ins;
			ReferenceType receiver;
	
			return "caller".equals(invoke.getMethodName(cpg))
				&& TAKAMAKA_CALLER_SIG.equals(invoke.getSignature(cpg))
				&& (receiver = invoke.getReferenceType(cpg)) instanceof ObjectType
				&& classLoader.isContract(((ObjectType) receiver).getClassName());
		}
		else
			return false;
	}
}