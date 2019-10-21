package takamaka.instrumentation.internal.checks.onMethods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;

import takamaka.instrumentation.IncompleteClasspathError;
import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.InconsistentEntryError;

/**
 * A checks that {@code @@Entry} methods only redefine {@code @@Entry} methods and that
 * {@code @@Entry} methods are only redefined by {@code @@Entry} methods. Moreover,
 * the kind of contract allowed in entries can only be enlarged in subclasses.
 */
public class EntryCodeIsConsistentWithClassHierarchyCheck extends VerifiedClassGen.ClassVerification.MethodVerification.Check {

	public EntryCodeIsConsistentWithClassHierarchyCheck(VerifiedClassGen.ClassVerification.MethodVerification verification) {
		verification.super();

		if (!Const.CONSTRUCTOR_NAME.equals(methodName) && !method.isPrivate()) {
			Optional<Class<?>> contractTypeForEntry = classLoader.isEntry(className, methodName, methodArgs, methodReturnType);
	
			IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				isIdenticallyEntryInSupertypesOf(classLoader.loadClass(className), contractTypeForEntry);
			});
		}
	}

	private void isIdenticallyEntryInSupertypesOf(Class<?> clazz, Optional<Class<?>> contractTypeForEntry) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == classLoader.bcelToClass(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(methodArgs)))
				.anyMatch(m -> !compatibleEntries(contractTypeForEntry, classLoader.isEntry(clazz.getName(), methodName, methodArgs, methodReturnType))))
			issue(new InconsistentEntryError(this.clazz, methodName, clazz.getName()));

		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyEntryInSupertypesOf(superclass, contractTypeForEntry);

		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyEntryInSupertypesOf(interf, contractTypeForEntry);
	}

	/**
	 * Determines if an entry annotation for a given method in a subclass is compatible with the entry annotation
	 * for a method overridden in a superclass by that method.
	 * 
	 * @param contractTypeInSubclass the type of contracts allowed by the annotation in the subclass
	 * @param contractTypeInSuperclass the type of contracts allowed by the annotation in the superclass
	 * @return true if and only both types are {@code null} or (both are non-{@code null} and
	 *         {@code contractTypeInSubclass} is a non-strict superclass of {@code contractTypeInSuperclass})
	 */
	private boolean compatibleEntries(Optional<Class<?>> contractTypeInSubclass, Optional<Class<?>> contractTypeInSuperclass) {
		if (!contractTypeInSubclass.isPresent() && !contractTypeInSuperclass.isPresent())
			return true;
		else
			return contractTypeInSubclass.isPresent() && contractTypeInSuperclass.isPresent()
					&& contractTypeInSubclass.get().isAssignableFrom(contractTypeInSuperclass.get());
	}
}