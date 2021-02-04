package io.takamaka.code.verification.internal.checksOnMethods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.InconsistentFromContractError;

/**
 * A checks that {@code @@FromContract} methods only redefine {@code @@FromContract} methods and that
 * {@code @@FromContract} methods are only redefined by {@code @@FromContract} methods. Moreover,
 * the kind of contract allowed in entries can only be enlarged in subclasses.
 */
public class FromContractCodeIsConsistentWithClassHierarchyCheck extends CheckOnMethods {

	public FromContractCodeIsConsistentWithClassHierarchyCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (!Const.CONSTRUCTOR_NAME.equals(methodName) && !method.isPrivate()) {
			Optional<Class<?>> contractTypeForEntry = annotations.getFromContractArgument(className, methodName, methodArgs, methodReturnType);
	
			ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyFromContractInSupertypesOf(classLoader.loadClass(className), contractTypeForEntry);
			});
		}
	}

	private void isIdenticallyFromContractInSupertypesOf(Class<?> clazz, Optional<Class<?>> contractTypeForEntry) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == bcelToClass.of(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), bcelToClass.of(methodArgs)))
				.anyMatch(m -> !compatibleFromContracts(contractTypeForEntry, annotations.getFromContractArgument(clazz.getName(), methodName, methodArgs, methodReturnType))))
			issue(new InconsistentFromContractError(inferSourceFile(), methodName, clazz.getName()));

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
		if (!contractTypeInSubclass.isPresent() && !contractTypeInSuperclass.isPresent())
			return true;
		else
			return contractTypeInSubclass.isPresent() && contractTypeInSuperclass.isPresent()
				&& contractTypeInSubclass.get().isAssignableFrom(contractTypeInSuperclass.get());
	}
}