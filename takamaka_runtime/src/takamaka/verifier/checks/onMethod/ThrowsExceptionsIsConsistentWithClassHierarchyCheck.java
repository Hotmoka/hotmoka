package takamaka.verifier.checks.onMethod;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.InconsistentThrowsExceptionsError;

/**
 * A check that {@code @@ThrowsExceptions} methods only redefine {@code @@ThrowsExceptions} methods and that
 * {@code @@ThrowsExceptions} methods are only redefined by {@code @@ThrowsExceptions} methods.
 */
public class ThrowsExceptionsIsConsistentWithClassHierarchyCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public ThrowsExceptionsIsConsistentWithClassHierarchyCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		if (!method.getName().equals(Const.CONSTRUCTOR_NAME) && method.isPublic()) {
			boolean wasThrowsExceptions = classLoader.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
	
			IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyThrowsExceptionsInSupertypesOf(classLoader.loadClass(className), method, wasThrowsExceptions);
			});
		}
	}

	private void isIdenticallyThrowsExceptionsInSupertypesOf(Class<?> clazz, Method method, boolean wasThrowsExceptions) {
		String name = method.getName();
		Type returnType = method.getReturnType();
		Type[] args = method.getArgumentTypes();
	
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(name) && m.getReturnType() == classLoader.bcelToClass(returnType)
						&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(args)))
				.anyMatch(m -> wasThrowsExceptions != classLoader.isThrowsExceptions(clazz.getName(), name, args, returnType)))
			issue(new InconsistentThrowsExceptionsError(this.clazz, method, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyThrowsExceptionsInSupertypesOf(superclass, method, wasThrowsExceptions);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyThrowsExceptionsInSupertypesOf(interf, method, wasThrowsExceptions);
	}
}