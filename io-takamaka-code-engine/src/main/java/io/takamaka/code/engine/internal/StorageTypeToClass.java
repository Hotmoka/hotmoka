package io.takamaka.code.engine.internal;

import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.takamaka.code.engine.internal.transactions.AbstractResponseBuilder;

/**
 * An object that translates storage types into their run-time class tag.
 */
public class StorageTypeToClass {

	/**
	 * The class loader that can be used to load classes for the transaction being performed.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * Builds an object that translates storage types into their run-time class tag,
	 * by using the class loader of the given response builder.
	 * 
	 * @param builder the builder of the transaction for which the translation is performed
	 */
	public StorageTypeToClass(AbstractResponseBuilder<?,?> builder) {
		this.classLoader = builder.classLoader;
	}

	/**
	 * Builds an object that translates storage types into their run-time class tag,
	 * by using a given class loader.
	 * 
	 * @param classLoader the class loader to use
	 */
	public StorageTypeToClass(EngineClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Yields the class object that represents the given storage type in the Java language,
	 * for the current transaction.
	 * 
	 * @param type the storage type
	 * @return the class object, if any
	 * @throws ClassNotFoundException if some class type cannot be found
	 */
	public Class<?> toClass(StorageType type) throws ClassNotFoundException {
		if (type instanceof BasicTypes) {
			switch ((BasicTypes) type) {
			case BOOLEAN: return boolean.class;
			case BYTE: return byte.class;
			case CHAR: return char.class;
			case SHORT: return short.class;
			case INT: return int.class;
			case LONG: return long.class;
			case FLOAT: return float.class;
			case DOUBLE: return double.class;
			}
		}
		else if (type instanceof ClassType)
			return classLoader.loadClass(((ClassType) type).name);
	
		throw new IllegalArgumentException("unexpected storage type");
	}
}