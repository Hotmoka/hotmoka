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

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.InconsistentThrowsExceptionsError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that {@code @@ThrowsExceptions} methods only redefine {@code @@ThrowsExceptions} methods and that
 * {@code @@ThrowsExceptions} methods are only redefined by {@code @@ThrowsExceptions} methods.
 */
public class ThrowsExceptionsIsConsistentWithClassHierarchyCheck extends CheckOnMethods {

	public ThrowsExceptionsIsConsistentWithClassHierarchyCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws IllegalJarException {
		super(builder, method);

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && method.isPublic()) {
			try {
				boolean wasThrowsExceptions = methodIsThrowsExceptionsIn(className);
				Class<?> rt = bcelToClass.of(methodReturnType);
				Class<?>[] args = bcelToClass.of(methodArgs);
				isIdenticallyThrowsExceptionsInSupertypesOf(clazz, wasThrowsExceptions, rt, args);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalJarException(e);
			}
		}
	}

	private void isIdenticallyThrowsExceptionsInSupertypesOf(Class<?> clazz, boolean wasThrowsExceptions, Class<?> rt, Class<?>[] args) throws IllegalJarException {
		for (var method: clazz.getDeclaredMethods())
			if (!Modifier.isPrivate(method.getModifiers()) && method.getName().equals(methodName) && method.getReturnType() == rt && Arrays.equals(method.getParameterTypes(), args)
					&& wasThrowsExceptions != methodIsThrowsExceptionsIn(clazz.getName()))
				issue(new InconsistentThrowsExceptionsError(inferSourceFile(), methodName, clazz.getName()));
				
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyThrowsExceptionsInSupertypesOf(superclass, wasThrowsExceptions, rt, args);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyThrowsExceptionsInSupertypesOf(interf, wasThrowsExceptions, rt, args);
	}
}