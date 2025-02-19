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

import static io.hotmoka.exceptions.CheckRunnable.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.CallerNotOnThisError;
import io.hotmoka.verification.errors.CallerOutsideFromContractError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;
import io.takamaka.code.constants.Constants;

/**
 * A check that {@code caller()} is only used with {@code this} as receiver
 * and inside an {@code @@FromContract} method or constructor.
 */
public class CallerIsUsedOnThisAndInFromContractCheck extends CheckOnMethods {

	public CallerIsUsedOnThisAndInFromContractCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws IllegalJarException {
		super(builder, method);

		boolean isFromAnnotatedAsContract;

		try {
			isFromAnnotatedAsContract = annotations.isFromContract(className, methodName, methodArgs, methodReturnType);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}

		boolean isFromContract = isFromAnnotatedAsContract || bootstraps.isPartOfFromContract(method);

		check(IllegalJarException.class, () ->
			instructions()
				.filter(uncheck(IllegalJarException.class, this::isCallToStorageCaller))
				.forEach(ih -> {
					if (!isFromContract)
						issue(new CallerOutsideFromContractError(inferSourceFile(), methodName, lineOf(ih)));

					if (!previousIsLoad0(ih))
						issue(new CallerNotOnThisError(inferSourceFile(), methodName, lineOf(ih)));
				})
		);
	}

	private boolean previousIsLoad0(InstructionHandle ih) {
		// we skip NOPs
		for (ih = ih.getPrev(); ih != null && ih.getInstruction() instanceof NOP; ih = ih.getPrev());

		return ih != null && ih.getInstruction() instanceof LoadInstruction li && li.getIndex() == 0;
	}

	/**
	 * The Java bytecode types of the {@code caller()} method of {@link io.takamaka.code.lang.Storage}.
	 */
	private final static String TAKAMAKA_CALLER_SIG = "()L" + Constants.CONTRACT_NAME.replace('.', '/') + ";";

	private boolean isCallToStorageCaller(InstructionHandle ih) throws IllegalJarException {
		try {
			return ih.getInstruction() instanceof InvokeInstruction invoke
				&& "caller".equals(invoke.getMethodName(cpg))
				&& TAKAMAKA_CALLER_SIG.equals(invoke.getSignature(cpg))
				&& invoke.getReferenceType(cpg) instanceof ObjectType receiver
				&& classLoader.isStorage(receiver.getClassName());
		}
		catch (ClassNotFoundException e) {
			throw new IllegalJarException(e);
		}
	}
}