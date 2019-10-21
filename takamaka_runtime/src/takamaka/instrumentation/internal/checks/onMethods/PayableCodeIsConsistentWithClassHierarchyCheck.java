package takamaka.instrumentation.internal.checks.onMethods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;

import takamaka.instrumentation.IncompleteClasspathError;
import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.InconsistentPayableError;

/**
 * A check that {@code @@Payable} methods only redefine {@code @@Payable} methods and that
 * {@code @@Payable} methods are only redefined by {@code @@Payable} methods.
 */
public class PayableCodeIsConsistentWithClassHierarchyCheck extends VerifiedClassGen.ClassVerification.MethodVerification.Check {

	public PayableCodeIsConsistentWithClassHierarchyCheck(VerifiedClassGen.ClassVerification.MethodVerification verification) {
		verification.super();

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate()) {
			boolean wasPayable = classLoader.isPayable(className, methodName, methodArgs, methodReturnType);
	
			IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyPayableInSupertypesOf(classLoader.loadClass(className), wasPayable);
			});
		}
	}

	private void isIdenticallyPayableInSupertypesOf(Class<?> clazz, boolean wasPayable) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == classLoader.bcelToClass(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(methodArgs)))
				.anyMatch(m -> wasPayable != classLoader.isPayable(clazz.getName(), methodName, methodArgs, methodReturnType)))
			issue(new InconsistentPayableError(this.clazz, methodName, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyPayableInSupertypesOf(superclass, wasPayable);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyPayableInSupertypesOf(interf, wasPayable);
	}
}