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

package io.hotmoka.verification.internal;

import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.Type;

/**
 * A verification check on a specific method of a class.
 */
public abstract class CheckOnMethods extends CheckOnClasses {
	protected final MethodGen method;
	protected final String methodName;
	protected final Type[] methodArgs;
	protected final Type methodReturnType;
	protected final boolean isConstructorOfInnerNonStaticClass;

	/**
	 * Builds the verification check.
	 * 
	 * @param builder the verification context
	 * @param method the method to verify
	 */
	protected CheckOnMethods(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder);

		this.method = method;
		this.methodName = method.getName();
		this.methodArgs = method.getArgumentTypes();
		this.methodReturnType = method.getReturnType();
		this.isConstructorOfInnerNonStaticClass = isConstructorOfInstanceInnerClass();
	}

	private boolean isConstructorOfInstanceInnerClass() {
		int dollarPos = className.lastIndexOf('$');

		// constructors of inner classes c have a first implicit parameter whose type t is the parent class
		// and they start with aload_0 aload_1 putfield c.f:t
		if (dollarPos > 0 && Const.CONSTRUCTOR_NAME.equals(method.getName())
				&& methodArgs.length > 0 && methodArgs[0] instanceof ObjectType ot
				&& ot.getClassName().equals(className.substring(0, dollarPos))) {

			InstructionList il = method.getInstructionList();
			if (il != null && il.getLength() >= 3) {
				Instruction[] instructions = il.getInstructions();

				return instructions[0] instanceof LoadInstruction li0 && li0.getIndex() == 0
						&& instructions[1] instanceof LoadInstruction li1 && li1.getIndex() == 1
						&& instructions[2] instanceof PUTFIELD putfield && putfield.getFieldType(cpg).equals(ot)
						&& putfield.getReferenceType(cpg) instanceof ObjectType pot && pot.getClassName().equals(className);
			}
		}

		return false;
	}

	/**
	 * Yields the instructions of the method under verification.
	 * 
	 * @return the instructions
	 */
	protected final Stream<InstructionHandle> instructions() {
		return instructionsOf(method);
	}

	/**
	 * Yields the source line number from which the given instruction of the method under verification was compiled.
	 * 
	 * @param ih the instruction
	 * @return the line number, or -1 if not available
	 */
	protected final int lineOf(InstructionHandle ih) {
		return lineOf(method, ih);
	}

	/**
	 * Yields the source line number for the instruction at the given program point of the method under verification.
	 * 
	 * @param pc the program point
	 * @return the line number, or -1 if not available
	 */
	protected final int lineOf(int pc) {
		return lineOf(method, pc);
	}
}