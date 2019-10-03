package takamaka.verifier.checks;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.InconsistentEntryError;

/**
 * A checks that {@code @@Entry} methods only redefine {@code @@Entry} methods and that
 * {@code @@Entry} methods are only redefined by {@code @@Entry} methods. Moreover,
 * the kind of contract allowed in entries can only be enlarged in subclasses.
 */
public class EntryIsConsistentAlongSubclassesCheck extends VerifiedClassGen.ClassVerification.ClassLevelCheck {

	public EntryIsConsistentAlongSubclassesCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		Stream.of(clazz.getMethods())
			.filter(method -> !method.getName().equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate())
			.forEachOrdered(method -> {
				Class<?> contractTypeForEntry = classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType());
	
				IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
					isIdenticallyEntryInSupertypesOf(classLoader.loadClass(className), method, contractTypeForEntry);
				});
			});
	}

	private void isIdenticallyEntryInSupertypesOf(Class<?> clazz, Method method, Class<?> contractTypeForEntry) {
		String name = method.getName();
		Type returnType = method.getReturnType();
		Type[] args = method.getArgumentTypes();

		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(name) && m.getReturnType() == classLoader.bcelToClass(returnType)
						&& Arrays.equals(m.getParameterTypes(), classLoader.bcelToClass(args)))
				.anyMatch(m -> !compatibleEntries(contractTypeForEntry, classLoader.isEntry(clazz.getName(), name, args, returnType))))
			issue(new InconsistentEntryError(this.clazz, method, clazz.getName()));

		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyEntryInSupertypesOf(superclass, method, contractTypeForEntry);

		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyEntryInSupertypesOf(interf, method, contractTypeForEntry);
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
	private boolean compatibleEntries(Class<?> contractTypeInSubclass, Class<?> contractTypeInSuperclass) {
		if (contractTypeInSubclass == null && contractTypeInSuperclass == null)
			return true;
		else
			return contractTypeInSubclass != null && contractTypeInSuperclass != null && contractTypeInSubclass.isAssignableFrom(contractTypeInSuperclass);
	}
}