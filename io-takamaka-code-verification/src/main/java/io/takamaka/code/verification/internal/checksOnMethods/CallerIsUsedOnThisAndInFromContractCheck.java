package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import io.takamaka.code.constants.Constants;
import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.CallerNotOnThisError;
import io.takamaka.code.verification.issues.CallerOutsideFromContractError;

/**
 * A check that {@code caller()} is only used with {@code this} as receiver
 * and inside an {@code @@FromContract} method or constructor.
 */
public class CallerIsUsedOnThisAndInFromContractCheck extends CheckOnMethods {

	public CallerIsUsedOnThisAndInFromContractCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		boolean isFromContract = annotations.isFromContract(className, methodName, methodArgs, methodReturnType) || bootstraps.isPartOfFromContract(method);

		instructions()
			.filter(this::isCallToStorageCaller)
			.forEach(ih -> {
				if (!isFromContract)
					issue(new CallerOutsideFromContractError(inferSourceFile(), methodName, lineOf(ih)));

				if (!previousIsLoad0(ih))
					issue(new CallerNotOnThisError(inferSourceFile(), methodName, lineOf(ih)));
			});
	}

	private boolean previousIsLoad0(InstructionHandle ih) {
		// we skip NOPs
		for (ih = ih.getPrev(); ih != null && ih.getInstruction() instanceof NOP; ih = ih.getPrev());

		Instruction ins;
		return ih != null && (ins = ih.getInstruction()) instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0;
	}

	/**
	 * The Java bytecode types of the {@code caller()} method of {@link io.takamaka.code.lang.Storage}.
	 */
	private final static String TAKAMAKA_CALLER_SIG = "()L" + Constants.CONTRACT_NAME.replace('.', '/') + ";";

	private boolean isCallToStorageCaller(InstructionHandle ih) {
		Instruction ins = ih.getInstruction();
		if (ins instanceof InvokeInstruction) {
			InvokeInstruction invoke = (InvokeInstruction) ins;
			ReferenceType receiver;

			return "caller".equals(invoke.getMethodName(cpg))
				&& TAKAMAKA_CALLER_SIG.equals(invoke.getSignature(cpg))
				&& (receiver = invoke.getReferenceType(cpg)) instanceof ObjectType
				&& classLoader.isStorage(((ObjectType) receiver).getClassName());
		}
		else
			return false;
	}
}