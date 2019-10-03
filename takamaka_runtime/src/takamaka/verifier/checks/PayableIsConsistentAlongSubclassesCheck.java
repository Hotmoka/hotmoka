package takamaka.verifier.checks;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.InconsistentPayableError;

/**
 * A check that {@code @@Payable} methods only redefine {@code @@Payable} methods and that
 * {@code @@Payable} methods are only redefined by {@code @@Payable} methods.
 */
public class PayableIsConsistentAlongSubclassesCheck extends VerifiedClassGen.ClassVerification.ClassLevelCheck {

	public PayableIsConsistentAlongSubclassesCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		Stream.of(clazz.getMethods())
			.filter(method -> !method.getName().equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate())
			.forEachOrdered(method -> {
				boolean wasPayable = classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
	
				IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
					isIdenticallyPayableInSupertypesOf(classLoader.loadClass(className), method, wasPayable);
				});
			});
	}

	private void isIdenticallyPayableInSupertypesOf(Class<?> clazz, Method method, boolean wasPayable) {
		String name = method.getName();
		Type returnType = method.getReturnType();
		Type[] args = method.getArgumentTypes();
	
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(name) && m.getReturnType() == classLoader.bcelToClass(returnType)
						&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(args)))
				.anyMatch(m -> wasPayable != classLoader.isPayable(clazz.getName(), name, args, returnType)))
			issue(new InconsistentPayableError(this.clazz, method, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyPayableInSupertypesOf(superclass, method, wasPayable);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyPayableInSupertypesOf(interf, method, wasPayable);
	}
}