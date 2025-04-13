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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.errors.InconsistentFromContractError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A checks that {@code @@FromContract} methods only redefine {@code @@FromContract} methods and that
 * {@code @@FromContract} methods are only redefined by {@code @@FromContract} methods. Moreover,
 * the kind of contract allowed in entries can only be enlarged in subclasses.
 */
public class FromContractCodeIsConsistentWithClassHierarchyCheck extends CheckOnMethods {

	public FromContractCodeIsConsistentWithClassHierarchyCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws UnknownTypeException {
		super(builder, method);

		if (!Const.CONSTRUCTOR_NAME.equals(methodName) && !method.isPrivate())
			isIdenticallyFromContractInSupertypesOf(clazz, getMethodFromContractArgumentIn(className));
	}

	private void isIdenticallyFromContractInSupertypesOf(Class<?> clazz, Optional<Class<?>> contractTypeForEntry) throws UnknownTypeException {
		try {
			for (Method method: clazz.getDeclaredMethods())
				if (!Modifier.isPrivate(method.getModifiers()) && method.getName().equals(methodName) && method.getReturnType() == methodReturnTypeClass && Arrays.equals(method.getParameterTypes(), methodArgsClasses)
				&& !compatibleFromContracts(contractTypeForEntry, getMethodFromContractArgumentIn(clazz.getName())))
					issue(new InconsistentFromContractError(inferSourceFile(), methodName, clazz.getName()));
		}
		catch (NoClassDefFoundError e) {
			throw new UnknownTypeException(e.getMessage());
		}
			
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyFromContractInSupertypesOf(superclass, contractTypeForEntry);

		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyFromContractInSupertypesOf(interf, contractTypeForEntry);
	}

	/**
	 * Determines if a {@code @@FromContract} annotation for a given method in a subclass is compatible with
	 * the {@code @@FromContract} annotation for a method overridden in a superclass by that method.
	 * 
	 * @param contractTypeInSubclass the type of contracts allowed by the annotation in the subclass
	 * @param contractTypeInSuperclass the type of contracts allowed by the annotation in the superclass
	 * @return true if and only both types are {@code null} or (both are non-{@code null} and
	 *         {@code contractTypeInSubclass} is a non-strict superclass of {@code contractTypeInSuperclass})
	 */
	private boolean compatibleFromContracts(Optional<Class<?>> contractTypeInSubclass, Optional<Class<?>> contractTypeInSuperclass) {
		if (contractTypeInSubclass.isEmpty() && contractTypeInSuperclass.isEmpty())
			return true;
		else
			return contractTypeInSubclass.isPresent() && contractTypeInSuperclass.isPresent()
				&& contractTypeInSubclass.get().isAssignableFrom(contractTypeInSuperclass.get());
	}
}