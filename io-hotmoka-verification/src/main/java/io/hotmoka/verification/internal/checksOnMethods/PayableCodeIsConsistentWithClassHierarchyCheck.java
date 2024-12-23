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

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.errors.InconsistentPayableError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that {@code @@Payable} methods only redefine {@code @@Payable} methods and that
 * {@code @@Payable} methods are only redefined by {@code @@Payable} methods.
 */
public class PayableCodeIsConsistentWithClassHierarchyCheck extends CheckOnMethods {

	public PayableCodeIsConsistentWithClassHierarchyCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws ClassNotFoundException {
		super(builder, method);

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate()) {
			boolean wasPayable = annotations.isPayable(className, methodName, methodArgs, methodReturnType);
			isIdenticallyPayableInSupertypesOf(classLoader.loadClass(className), wasPayable);
		}
	}

	private void isIdenticallyPayableInSupertypesOf(Class<?> clazz, boolean wasPayable) throws ClassNotFoundException {
		Class<?> rt = bcelToClass.of(methodReturnType);
		Class<?>[] args = bcelToClass.of(methodArgs);
		Stream<Method> methods = Stream.of(clazz.getDeclaredMethods())
			.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == rt
						&& Arrays.equals(m.getParameterTypes(), args));

		if (check(ClassNotFoundException.class, () ->
			methods.anyMatch(uncheck(ClassNotFoundException.class, m -> wasPayable != annotations.isPayable(clazz.getName(), methodName, methodArgs, methodReturnType)))))
			issue(new InconsistentPayableError(inferSourceFile(), methodName, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyPayableInSupertypesOf(superclass, wasPayable);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyPayableInSupertypesOf(interf, wasPayable);
	}
}