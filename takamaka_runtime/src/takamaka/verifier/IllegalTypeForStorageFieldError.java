package takamaka.verifier;

import java.lang.reflect.Field;

import org.apache.bcel.generic.ClassGen;

public class IllegalTypeForStorageFieldError extends Error {

	public IllegalTypeForStorageFieldError(ClassGen clazz, Field where) {
		super(clazz, where, "type not allowed for a field of a storage class"
			+ (where.getType().isEnum() ? ": it is an enumeration with instance non-transient fields" : ""));
	}
}