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

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.errors.InconsistentThrowsExceptionsError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that {@code @@ThrowsExceptions} methods only redefine {@code @@ThrowsExceptions} methods and that
 * {@code @@ThrowsExceptions} methods are only redefined by {@code @@ThrowsExceptions} methods.
 */
public class ThrowsExceptionsIsConsistentWithClassHierarchyCheck extends CheckOnMethods {

	public ThrowsExceptionsIsConsistentWithClassHierarchyCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws UnknownTypeException {
		super(builder, method);

		if (!Const.CONSTRUCTOR_NAME.equals(methodName) && method.isPublic())
			isIdenticallyThrowsExceptionsInSupertypesOf(clazz, methodIsThrowsExceptionsIn(className), methodReturnTypeClass, methodArgsClasses);
	}

	private void isIdenticallyThrowsExceptionsInSupertypesOf(Class<?> clazz, boolean wasThrowsExceptions, Class<?> rt, Class<?>[] args) throws UnknownTypeException {
		for (var method: clazz.getDeclaredMethods())
			if (!Modifier.isPrivate(method.getModifiers()) && methodName.equals(method.getName())
					&& method.getReturnType() == rt && Arrays.equals(method.getParameterTypes(), args)
					&& wasThrowsExceptions != methodIsThrowsExceptionsIn(clazz.getName()))
				issue(new InconsistentThrowsExceptionsError(inferSourceFile(), methodName, clazz.getName()));
				
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyThrowsExceptionsInSupertypesOf(superclass, wasThrowsExceptions, rt, args);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyThrowsExceptionsInSupertypesOf(interf, wasThrowsExceptions, rt, args);
	}
}