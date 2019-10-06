package takamaka.verifier.checks.onMethod;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.InconsistentThrowsExceptionsError;

/**
 * A check that {@code @@ThrowsExceptions} methods only redefine {@code @@ThrowsExceptions} methods and that
 * {@code @@ThrowsExceptions} methods are only redefined by {@code @@ThrowsExceptions} methods.
 */
public class ThrowsExceptionsIsConsistentWithClassHierarchyCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public ThrowsExceptionsIsConsistentWithClassHierarchyCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && method.isPublic()) {
			boolean wasThrowsExceptions = classLoader.isThrowsExceptions(className, methodName, methodArgs, methodReturnType);
	
			IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyThrowsExceptionsInSupertypesOf(classLoader.loadClass(className), wasThrowsExceptions);
			});
		}
	}

	private void isIdenticallyThrowsExceptionsInSupertypesOf(Class<?> clazz, boolean wasThrowsExceptions) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == classLoader.bcelToClass(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(methodArgs)))
				.anyMatch(m -> wasThrowsExceptions != classLoader.isThrowsExceptions(clazz.getName(), methodName, methodArgs, methodReturnType)))
			issue(new InconsistentThrowsExceptionsError(this.clazz, methodName, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyThrowsExceptionsInSupertypesOf(superclass, wasThrowsExceptions);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyThrowsExceptionsInSupertypesOf(interf, wasThrowsExceptions);
	}
}