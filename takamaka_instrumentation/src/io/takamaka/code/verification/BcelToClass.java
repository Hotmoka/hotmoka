package io.takamaka.code.verification;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.internal.ThrowIncompleteClasspathError;

/**
 * A utility that transforms a BCEL type into its corresponding class tag.
 */
public class BcelToClass {

	/**
	 * The class with whose class loader the transformation is performed.
	 */
	private final VerifiedClass clazz;

	/**
	 * Builds the utility object.
	 * 
	 * @param clazz the class for whose class loader the transformation is performed
	 */
	BcelToClass(VerifiedClass clazz) {
		this.clazz = clazz;
	}

	/**
	 * Computes the Java class tag for the given BCEL type.
	 * 
	 * @param type the BCEL type
	 * @return the class tag corresponding to {@code type}
	 */
	public final Class<?> of(Type type) {
		if (type == BasicType.BOOLEAN)
			return boolean.class;
		else if (type == BasicType.BYTE)
			return byte.class;
		else if (type == BasicType.CHAR)
			return char.class;
		else if (type == BasicType.DOUBLE)
			return double.class;
		else if (type == BasicType.FLOAT)
			return float.class;
		else if (type == BasicType.INT)
			return int.class;
		else if (type == BasicType.LONG)
			return long.class;
		else if (type == BasicType.SHORT)
			return short.class;
		else if (type == BasicType.VOID)
			return void.class;
		else if (type instanceof ObjectType)
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> clazz.classLoader.loadClass(type.toString()));
		else { // array
			Class<?> elementsClass = of(((ArrayType) type).getElementType());
			// trick: we build an array of 0 elements just to access its class token
			return java.lang.reflect.Array.newInstance(elementsClass, 0).getClass();
		}
	}

	/**
	 * Computes the Java class tags for the given BCEL types.
	 * 
	 * @param types the BCEL types
	 * @return the class tags corresponding to {@code types}
	 */
	public final Class<?>[] of(Type[] types) {
		return Stream.of(types)
			.map(this::of)
			.collect(Collectors.toList())
			.toArray(new Class<?>[types.length]);
	}
}