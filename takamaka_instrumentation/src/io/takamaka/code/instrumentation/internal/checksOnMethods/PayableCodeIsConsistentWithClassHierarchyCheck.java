package io.takamaka.code.instrumentation.internal.checksOnMethods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;

import io.takamaka.code.instrumentation.internal.ThrowIncompleteClasspathError;
import io.takamaka.code.instrumentation.internal.VerifiedClass;
import io.takamaka.code.instrumentation.issues.InconsistentPayableError;

/**
 * A check that {@code @@Payable} methods only redefine {@code @@Payable} methods and that
 * {@code @@Payable} methods are only redefined by {@code @@Payable} methods.
 */
public class PayableCodeIsConsistentWithClassHierarchyCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public PayableCodeIsConsistentWithClassHierarchyCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate()) {
			boolean wasPayable = clazz.annotations.isPayable(className, methodName, methodArgs, methodReturnType);
	
			ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyPayableInSupertypesOf(classLoader.loadClass(className), wasPayable);
			});
		}
	}

	private void isIdenticallyPayableInSupertypesOf(Class<?> clazz, boolean wasPayable) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == this.clazz.bcelToClass.of(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), this.clazz.bcelToClass.of(methodArgs)))
				.anyMatch(m -> wasPayable != this.clazz.annotations.isPayable(clazz.getName(), methodName, methodArgs, methodReturnType)))
			issue(new InconsistentPayableError(inferSourceFile(), methodName, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyPayableInSupertypesOf(superclass, wasPayable);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyPayableInSupertypesOf(interf, wasPayable);
	}
}