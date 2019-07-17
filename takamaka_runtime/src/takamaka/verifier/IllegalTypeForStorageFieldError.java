package takamaka.verifier;

import java.lang.reflect.Field;

import org.apache.bcel.generic.ClassGen;

public class IllegalTypeForStorageFieldError extends Error {

	public IllegalTypeForStorageFieldError(ClassGen clazz, Field where) {
		super(clazz, where, "Field " + where + " has a type not allowed for a field of a storage class");
	}
}