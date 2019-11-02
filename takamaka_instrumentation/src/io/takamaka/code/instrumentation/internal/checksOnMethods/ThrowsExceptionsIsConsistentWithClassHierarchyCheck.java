package io.takamaka.code.instrumentation.internal.checksOnMethods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;

import io.takamaka.code.instrumentation.internal.ThrowIncompleteClasspathError;
import io.takamaka.code.instrumentation.internal.VerifiedClass;
import io.takamaka.code.instrumentation.issues.InconsistentThrowsExceptionsError;

/**
 * A check that {@code @@ThrowsExceptions} methods only redefine {@code @@ThrowsExceptions} methods and that
 * {@code @@ThrowsExceptions} methods are only redefined by {@code @@ThrowsExceptions} methods.
 */
public class ThrowsExceptionsIsConsistentWithClassHierarchyCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public ThrowsExceptionsIsConsistentWithClassHierarchyCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && method.isPublic()) {
			boolean wasThrowsExceptions = clazz.annotations.isThrowsExceptions(className, methodName, methodArgs, methodReturnType);
	
			ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyThrowsExceptionsInSupertypesOf(classLoader.loadClass(className), wasThrowsExceptions);
			});
		}
	}

	private void isIdenticallyThrowsExceptionsInSupertypesOf(Class<?> clazz, boolean wasThrowsExceptions) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == this.clazz.bcelToClass.of(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), this.clazz.bcelToClass.of(methodArgs)))
				.anyMatch(m -> wasThrowsExceptions != this.clazz.annotations.isThrowsExceptions(clazz.getName(), methodName, methodArgs, methodReturnType)))
			issue(new InconsistentThrowsExceptionsError(inferSourceFile(), methodName, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyThrowsExceptionsInSupertypesOf(superclass, wasThrowsExceptions);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyThrowsExceptionsInSupertypesOf(interf, wasThrowsExceptions);
	}
}