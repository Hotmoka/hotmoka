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

package io.hotmoka.verification.internal.checksOnMethods;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;
import io.hotmoka.verification.issues.CallerNotOnThisError;
import io.hotmoka.verification.issues.CallerOutsideFromContractError;
import io.takamaka.code.constants.Constants;

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