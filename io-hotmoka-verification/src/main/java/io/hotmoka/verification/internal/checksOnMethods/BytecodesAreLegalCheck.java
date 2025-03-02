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
import org.apache.bcel.generic.JsrInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.StoreInstruction;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.IllegalJsrInstructionError;
import io.hotmoka.verification.errors.IllegalPutstaticInstructionError;
import io.hotmoka.verification.errors.IllegalRetInstructionError;
import io.hotmoka.verification.errors.IllegalSynchronizationError;
import io.hotmoka.verification.errors.IllegalUpdateOfLocal0Error;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that the method has no unusual bytecodes, such as {@code jsr}, {@code ret}
 * or updates of local 0 in instance methods. Such bytecodes are allowed in
 * Java bytecode, although they are never generated by modern compilers. Takamaka forbids them
 * since they make code verification more difficult. Moreover, this check forbids
 * calls to instrumentation code.
 */
public class BytecodesAreLegalCheck extends CheckOnMethods {

	public BytecodesAreLegalCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws IllegalJarException {
		super(builder, method);

		instructions().forEach(this::checkIfItIsIllegal);
	}

	private void checkIfItIsIllegal(InstructionHandle ih) {
		Instruction ins = ih.getInstruction();

		if (ins instanceof PUTSTATIC)
			issue(new IllegalPutstaticInstructionError(inferSourceFile(), methodName, lineOf(ih)));
		else if (ins instanceof JsrInstruction)
			issue(new IllegalJsrInstructionError(inferSourceFile(), methodName, lineOf(ih)));
		else if (ins instanceof RET)
			issue(new IllegalRetInstructionError(inferSourceFile(), methodName, lineOf(ih)));
		else if (!method.isStatic() && ins instanceof StoreInstruction si && si.getIndex() == 0)
			issue(new IllegalUpdateOfLocal0Error(inferSourceFile(), methodName, lineOf(ih)));					
		else if (ins instanceof MONITORENTER || ins instanceof MONITOREXIT)
			issue(new IllegalSynchronizationError(inferSourceFile(), methodName, lineOf(ih)));
	}
}