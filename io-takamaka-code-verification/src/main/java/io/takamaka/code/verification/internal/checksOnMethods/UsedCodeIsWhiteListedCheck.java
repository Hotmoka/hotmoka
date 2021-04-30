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

package io.takamaka.code.verification.internal.checksOnMethods;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalAccessToNonWhiteListedFieldError;
import io.takamaka.code.verification.issues.IllegalCallToNonWhiteListedConstructorError;
import io.takamaka.code.verification.issues.IllegalCallToNonWhiteListedMethodError;

/**
 * A check that a method calls white-listed methods only and accesses white-listed fields only.
 */
public class UsedCodeIsWhiteListedCheck extends CheckOnMethods {

	public UsedCodeIsWhiteListedCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		instructions().forEach(ih -> {
			Instruction ins = ih.getInstruction();
			if (ins instanceof FieldInstruction) {
				FieldInstruction fi = (FieldInstruction) ins;
				if (!hasWhiteListingModel(fi))
					issue(new IllegalAccessToNonWhiteListedFieldError(inferSourceFile(), methodName, lineOf(ih), fi.getLoadClassType(cpg).getClassName(), fi.getFieldName(cpg)));
			}
			else if (ins instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) ins;
				if (!hasWhiteListingModel(invoke)) {
					Optional<? extends Executable> target = resolver.resolvedExecutableFor(invoke);
					if (target.isPresent()) {
						Executable executable = target.get();
						if (executable instanceof Constructor<?>)
							issue(new IllegalCallToNonWhiteListedConstructorError(inferSourceFile(), methodName, lineOf(ih), executable.getDeclaringClass().getName()));
						else
							issue(new IllegalCallToNonWhiteListedMethodError(inferSourceFile(), methodName, lineOf(ih), executable.getDeclaringClass().getName(), executable.getName()));
					}
					else {
						// the call seems not resolvable
						ReferenceType receiverType = invoke.getReferenceType(cpg);
						String receiverClassName = receiverType instanceof ObjectType ? ((ObjectType) receiverType).getClassName() : "java.lang.Object";
						String methodName = invoke.getMethodName(cpg);

						if (invoke instanceof INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(methodName))
							issue(new IllegalCallToNonWhiteListedConstructorError(inferSourceFile(), methodName, lineOf(ih), receiverClassName));
						else
							issue(new IllegalCallToNonWhiteListedMethodError(inferSourceFile(), methodName, lineOf(ih), receiverClassName, methodName));
					}
				}
			}
		});
	}
}