package io.takamaka.code.verification.internal.checksOnMethods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.InconsistentRedPayableError;

/**
 * A check that {@code @@RedPayable} methods only redefine {@code @@RedPayable} methods and that
 * {@code @@RedPayable} methods are only redefined by {@code @@RedPayable} methods.
 */
public class RedPayableCodeIsConsistentWithClassHierarchyCheck extends CheckOnMethods {

	public RedPayableCodeIsConsistentWithClassHierarchyCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate()) {
			boolean wasRedPayable = annotations.isRedPayable(className, methodName, methodArgs, methodReturnType);
	
			ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyRedPayableInSupertypesOf(classLoader.loadClass(className), wasRedPayable);
			});
		}
	}

	private void isIdenticallyRedPayableInSupertypesOf(Class<?> clazz, boolean wasRedPayable) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == bcelToClass.of(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), bcelToClass.of(methodArgs)))
				.anyMatch(m -> wasRedPayable != annotations.isRedPayable(clazz.getName(), methodName, methodArgs, methodReturnType)))
			issue(new InconsistentRedPayableError(inferSourceFile(), methodName, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyRedPayableInSupertypesOf(superclass, wasRedPayable);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyRedPayableInSupertypesOf(interf, wasRedPayable);
	}
}