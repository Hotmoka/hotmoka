package io.takamaka.code.verification;

import org.apache.bcel.generic.Type;

/**
 * A utility that transforms a BCEL type into its corresponding class tag.
 */
public interface BcelToClass {

	/**
	 * Computes the Java class tag for the given BCEL type.
	 * 
	 * @param type the BCEL type
	 * @return the class tag corresponding to {@code type}
	 */
	Class<?> of(Type type);

	/**
	 * Computes the Java class tags for the given BCEL types.
	 * 
	 * @param types the BCEL types
	 * @return the class tags corresponding to {@code types}
	 */
	Class<?>[] of(Type[] types);
}