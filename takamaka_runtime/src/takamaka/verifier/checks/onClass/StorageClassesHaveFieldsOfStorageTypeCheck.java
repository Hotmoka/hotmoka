package takamaka.verifier.checks.onClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.stream.Stream;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalTypeForStorageFieldError;

/**
 * A checks that payable methods have an amount first argument.
 */
public class StorageClassesHaveFieldsOfStorageTypeCheck extends VerifiedClassGen.Verification.Check {

	public StorageClassesHaveFieldsOfStorageTypeCheck(VerifiedClassGen.Verification verification) {
		verification.super();

		if (classLoader.isStorage(className))
			IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				Stream.of(classLoader.loadClass(className).getDeclaredFields())
					.filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
					.filter(field -> !isTypeAllowedForStorageFields(field.getType()))
					.map(field -> new IllegalTypeForStorageFieldError(clazz, field.getName(), field.getType().isEnum()))
					.forEach(this::issue);
			});
	}

	@SuppressWarnings("unchecked")
	private boolean isTypeAllowedForStorageFields(Class<?> type) {
		// we allow Object since it can be the erasure of a generic type: the runtime of Takamaka
		// will check later if the actual type of the object in this field is allowed
		return type.isPrimitive() || type == Object.class || type == String.class || type == BigInteger.class
			|| (type.isEnum() && !hasInstanceFields((Class<? extends Enum<?>>) type))
			|| (!type.isArray() && classLoader.isStorage(type.getName()));
	}

	/**
	 * Determines if the given enumeration type has at least an instance, non-transient field.
	 * 
	 * @param clazz the class
	 * @return true only if that condition holds
	 */
	private static boolean hasInstanceFields(Class<? extends Enum<?>> clazz) {
		return Stream.of(clazz.getDeclaredFields())
			.map(Field::getModifiers)
			.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
	}
}